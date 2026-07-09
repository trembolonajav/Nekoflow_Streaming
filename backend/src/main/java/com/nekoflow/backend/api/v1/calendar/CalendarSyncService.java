package com.nekoflow.backend.api.v1.calendar;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.nekoflow.backend.config.AppProperties;

/**
 * Sincroniza o calendario com o cronograma de exibicao do AniList.
 *
 * Escopo restrito ao catalogo: consulta o nextAiringEpisode apenas dos animes
 * RELEASING com anilist_id e show_in_calendar = true. O que o admin ocultou
 * (show_in_calendar = false) e ignorado para sempre — a curadoria e uma flag,
 * nao uma delecao, entao o sync nunca readiciona.
 *
 * Para cada anime com proximo episodio anunciado, cria (ou reagenda) um
 * episodio placeholder SCHEDULED com scheduled_for = airingAt. Quando o
 * episodio real chega pelo worker, o upsert promove o placeholder a PUBLISHED.
 */
@Service
public class CalendarSyncService {

    private static final Logger log = LoggerFactory.getLogger(CalendarSyncService.class);
    private static final int BATCH_SIZE = 50;

    private static final String AIRING_QUERY = """
        query ($ids: [Int]) {
          Page(page: 1, perPage: 50) {
            media(id_in: $ids, type: ANIME) {
              id
              status
              nextAiringEpisode { episode airingAt }
            }
          }
        }
        """;

    private final JdbcTemplate jdbc;
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public CalendarSyncService(JdbcTemplate jdbc, RestTemplate restTemplate, AppProperties appProperties) {
        this.jdbc = jdbc;
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    public Map<String, Object> sync() {
        List<Map<String, Object>> candidates = jdbc.queryForList("""
            select id, anilist_id
            from anime
            where status = 'RELEASING'
              and show_in_calendar = true
              and anilist_id is not null
            """);

        int scheduledCreated = 0;
        int scheduledUpdated = 0;
        int finished = 0;
        int withoutSchedule = 0;

        Map<Long, UUID> animeByAnilistId = new LinkedHashMap<>();
        for (Map<String, Object> row : candidates) {
            animeByAnilistId.put(((Number) row.get("anilist_id")).longValue(), (UUID) row.get("id"));
        }

        List<Long> anilistIds = new ArrayList<>(animeByAnilistId.keySet());
        for (int from = 0; from < anilistIds.size(); from += BATCH_SIZE) {
            List<Long> batch = anilistIds.subList(from, Math.min(from + BATCH_SIZE, anilistIds.size()));
            JsonNode mediaList = fetchAiring(batch);
            for (JsonNode media : mediaList) {
                long anilistId = media.path("id").asLong();
                UUID animeId = animeByAnilistId.get(anilistId);
                if (animeId == null) {
                    continue;
                }

                String status = media.path("status").asText("");
                JsonNode next = media.path("nextAiringEpisode");
                if (next.isMissingNode() || next.isNull()) {
                    // AniList encerrou a exibicao: marca FINISHED para sair dos proximos syncs.
                    if ("FINISHED".equals(status) || "CANCELLED".equals(status)) {
                        jdbc.update("update anime set status = 'FINISHED', updated_at = now() where id = ?", animeId);
                        finished++;
                    } else {
                        withoutSchedule++;
                    }
                    continue;
                }

                int episodeNumber = next.path("episode").asInt();
                OffsetDateTime airingAt = OffsetDateTime.ofInstant(
                    Instant.ofEpochSecond(next.path("airingAt").asLong()), ZoneOffset.UTC);

                String existingStatus = jdbc.query(
                    "select status from episodes where anime_id = ? and number = ?",
                    rs -> rs.next() ? rs.getString(1) : null,
                    animeId, episodeNumber
                );

                if ("SCHEDULED".equals(existingStatus)) {
                    scheduledUpdated += jdbc.update("""
                        update episodes set scheduled_for = ?, updated_at = now()
                        where anime_id = ? and number = ? and status = 'SCHEDULED' and scheduled_for is distinct from ?
                        """, airingAt, animeId, episodeNumber, airingAt);
                } else if (existingStatus == null) {
                    jdbc.update("""
                        insert into episodes (id, anime_id, number, title, status, scheduled_for)
                        values (?, ?, ?, ?, 'SCHEDULED', ?)
                        """, UUID.randomUUID(), animeId, episodeNumber, "Episodio " + episodeNumber, airingAt);
                    scheduledCreated++;
                }
                // Episodio ja PUBLISHED/DRAFT com esse numero: o sync nao mexe.
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("animes", candidates.size());
        result.put("scheduled_created", scheduledCreated);
        result.put("scheduled_updated", scheduledUpdated);
        result.put("finished", finished);
        result.put("without_schedule", withoutSchedule);
        log.info("Calendar sync: {} animes, {} agendados novos, {} reagendados, {} finalizados.",
            candidates.size(), scheduledCreated, scheduledUpdated, finished);
        return result;
    }

    public Map<String, Object> setAnimeVisibility(UUID animeId, boolean show) {
        int updated = jdbc.update("update anime set show_in_calendar = ?, updated_at = now() where id = ?", show, animeId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Anime nao encontrado.");
        }
        if (!show) {
            // Oculto: remove os placeholders futuros para sumir do calendario na hora.
            // Episodios reais (PUBLISHED) nunca sao tocados.
            jdbc.update("delete from episodes where anime_id = ? and status = 'SCHEDULED'", animeId);
        }
        return Map.of("ok", true, "anime_id", animeId.toString(), "show_in_calendar", show);
    }

    private JsonNode fetchAiring(List<Long> anilistIds) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
            Map.of("query", AIRING_QUERY, "variables", Map.of("ids", anilistIds)),
            headers
        );

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                resolveEndpoint(), HttpMethod.POST, request, JsonNode.class);
            JsonNode body = response.getBody();
            if (body == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AniList retornou resposta vazia.");
            }
            JsonNode errors = body.path("errors");
            if (errors.isArray() && errors.size() > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AniList retornou erro: " + errors.path(0).path("message").asText("sem detalhes"));
            }
            return body.path("data").path("Page").path("media");
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao consultar AniList.", exception);
        }
    }

    private String resolveEndpoint() {
        AppProperties.Integrations integrations = appProperties.integrations();
        if (integrations != null && integrations.anilist() != null
            && integrations.anilist().endpoint() != null && !integrations.anilist().endpoint().isBlank()) {
            return integrations.anilist().endpoint();
        }
        String envValue = System.getenv("APP_ANILIST_ENDPOINT");
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return "https://graphql.anilist.co";
    }
}
