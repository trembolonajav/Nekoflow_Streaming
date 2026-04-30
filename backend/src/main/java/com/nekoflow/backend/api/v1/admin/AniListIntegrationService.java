package com.nekoflow.backend.api.v1.admin;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.nekoflow.backend.api.v1.admin.dto.AdminAniListSearchResponse;
import com.nekoflow.backend.config.AppProperties;

@Service
public class AniListIntegrationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String SEARCH_QUERY = """
        query ($search: String!, $perPage: Int!) {
          Page(page: 1, perPage: $perPage) {
            media(search: $search, type: ANIME, sort: SEARCH_MATCH) {
              id
              title { romaji english native }
              format
              status
              episodes
              seasonYear
              season
              coverImage { extraLarge large }
              bannerImage
              averageScore
              description(asHtml: false)
              genres
              studios(isMain: true) { nodes { name isAnimationStudio } }
            }
          }
        }
        """;

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public AniListIntegrationService(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    public List<AdminAniListSearchResponse> search(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() < 2) {
            return List.of();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
            Map.of(
                "query", SEARCH_QUERY,
                "variables", Map.of("search", trimmed, "perPage", 8)
            ),
            headers
        );

        try {
            ResponseEntity<AniListGraphQlResponse> response = restTemplate.exchange(
                resolveEndpoint(),
                HttpMethod.POST,
                request,
                AniListGraphQlResponse.class
            );

            AniListGraphQlResponse body = response.getBody();
            if (body == null) {
                return List.of();
            }
            if (body.errors() != null && !body.errors().isEmpty()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AniList retornou erro ao consultar metadados: " + extractAniListErrorMessage(body.errors())
                );
            }

            List<RawMedia> media = body.data() != null && body.data().page() != null
                ? body.data().page().media()
                : List.of();

            return media == null
                ? List.of()
                : media.stream()
                    .filter(Objects::nonNull)
                    .map(this::mapMedia)
                    .toList();
        } catch (HttpStatusCodeException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "AniList indisponivel: " + extractAniListErrorMessage(exception.getResponseBodyAsString())
            );
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao consultar AniList.", exception);
        }
    }

    private String resolveEndpoint() {
        AppProperties.Integrations integrations = appProperties.integrations();
        if (integrations != null && integrations.anilist() != null && integrations.anilist().endpoint() != null && !integrations.anilist().endpoint().isBlank()) {
            return integrations.anilist().endpoint();
        }

        String envValue = System.getenv("APP_ANILIST_ENDPOINT");
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return "https://graphql.anilist.co";
    }

    private AdminAniListSearchResponse mapMedia(RawMedia media) {
        return new AdminAniListSearchResponse(
            media.id(),
            coalesce(media.title() != null ? media.title().romaji() : null,
                media.title() != null ? media.title().english() : null,
                media.title() != null ? media.title().nativeTitle() : null,
                "—"),
            media.title() != null ? media.title().english() : null,
            media.title() != null ? media.title().nativeTitle() : null,
            media.format(),
            media.status(),
            media.episodes(),
            media.seasonYear(),
            media.season(),
            media.coverImage() != null ? coalesce(media.coverImage().extraLarge(), media.coverImage().large(), null) : null,
            media.bannerImage(),
            media.averageScore(),
            stripHtml(media.description()),
            media.genres() != null ? media.genres() : List.of(),
            media.studios() != null && media.studios().nodes() != null
                ? media.studios().nodes().stream()
                    .filter(Objects::nonNull)
                    .filter(node -> Boolean.TRUE.equals(node.isAnimationStudio()))
                    .map(StudioNode::name)
                    .filter(Objects::nonNull)
                    .toList()
                : List.of()
        );
    }

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        return html
            .replaceAll("(?i)<br\\s*/?>", "\n")
            .replaceAll("(?i)</p>", "\n\n")
            .replaceAll("<[^>]+>", "")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }

    private String extractAniListErrorMessage(List<Map<String, Object>> errors) {
        if (errors == null || errors.isEmpty()) {
            return "resposta sem detalhes.";
        }
        Object message = errors.get(0).get("message");
        return message == null || String.valueOf(message).isBlank()
            ? "resposta sem detalhes."
            : String.valueOf(message);
    }

    private String extractAniListErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "resposta sem detalhes.";
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode message = root.path("errors").path(0).path("message");
            if (!message.isMissingNode() && !message.asText().isBlank()) {
                return message.asText();
            }
        } catch (Exception ignored) {
            return responseBody.length() > 240 ? responseBody.substring(0, 240) : responseBody;
        }
        return "resposta sem detalhes.";
    }

    private String coalesce(String first, String second, String third, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        if (third != null && !third.isBlank()) {
            return third;
        }
        return fallback;
    }

    private String coalesce(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }

    private record AniListGraphQlResponse(
        AniListData data,
        List<Map<String, Object>> errors
    ) {
    }

    private record AniListData(
        @JsonProperty("Page") AniListPage page
    ) {
    }

    private record AniListPage(
        List<RawMedia> media
    ) {
    }

    private record RawMedia(
        Long id,
        RawTitle title,
        String format,
        String status,
        Integer episodes,
        Integer seasonYear,
        String season,
        RawCoverImage coverImage,
        String bannerImage,
        Integer averageScore,
        String description,
        List<String> genres,
        RawStudios studios
    ) {
    }

    private record RawTitle(
        String romaji,
        String english,
        @JsonProperty("native") String nativeTitle
    ) {
    }

    private record RawCoverImage(
        String extraLarge,
        String large
    ) {
    }

    private record RawStudios(
        List<StudioNode> nodes
    ) {
    }

    private record StudioNode(
        String name,
        Boolean isAnimationStudio
    ) {
    }
}
