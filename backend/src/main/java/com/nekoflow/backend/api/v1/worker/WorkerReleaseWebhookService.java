package com.nekoflow.backend.api.v1.worker;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.net.URLEncoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekoflow.backend.api.v1.worker.dto.WorkerReleaseResponse;
import com.nekoflow.backend.config.AppProperties;
import com.nekoflow.backend.domain.entity.AnimeEntity;
import com.nekoflow.backend.domain.entity.EpisodeEntity;
import com.nekoflow.backend.domain.entity.EpisodeVideoSourceEntity;
import com.nekoflow.backend.domain.enums.AnimeStatus;
import com.nekoflow.backend.domain.enums.AnimeType;
import com.nekoflow.backend.domain.enums.EpisodeStatus;
import com.nekoflow.backend.domain.enums.VideoProvider;
import com.nekoflow.backend.domain.enums.VideoSourceStatus;
import com.nekoflow.backend.domain.enums.VisibilityStatus;
import com.nekoflow.backend.domain.repository.AnimeRepository;
import com.nekoflow.backend.domain.repository.EpisodeRepository;
import com.nekoflow.backend.domain.repository.EpisodeVideoSourceRepository;

@Service
public class WorkerReleaseWebhookService {

    private static final String EVENT_RELEASE_PUBLISH = "release.publish";
    private static final String SEEK_EMBED_BASE = "https://nekoflow.seekplayer.me/#";

    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final RestTemplate restTemplate;
    private final AnimeRepository animeRepository;
    private final EpisodeRepository episodeRepository;
    private final EpisodeVideoSourceRepository episodeVideoSourceRepository;

    public WorkerReleaseWebhookService(
        ObjectMapper objectMapper,
        AppProperties appProperties,
        RestTemplate restTemplate,
        AnimeRepository animeRepository,
        EpisodeRepository episodeRepository,
        EpisodeVideoSourceRepository episodeVideoSourceRepository
    ) {
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.restTemplate = restTemplate;
        this.animeRepository = animeRepository;
        this.episodeRepository = episodeRepository;
        this.episodeVideoSourceRepository = episodeVideoSourceRepository;
    }

    @Transactional
    public WorkerReleaseResponse publishRelease(String rawBody, HttpHeaders headers) {
        verifySignature(rawBody, headers);

        JsonNode root = parseBody(rawBody);
        if (!EVENT_RELEASE_PUBLISH.equals(text(root, "event"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento de worker invalido.");
        }

        Long anilistId = longValue(root, "anilist_id");
        String title = firstNonBlank(text(root, "title"), titleFromAniList(root.path("anilist")));
        Integer episodeNumber = integerValue(root, "episode");
        String seekVideoId = text(root, "seek_video_id");

        if (isBlank(title)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Release sem titulo.");
        }
        if (episodeNumber == null || episodeNumber < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Release sem numero de episodio valido.");
        }

        AnimeResult animeResult = findOrCreateAnime(anilistId, title, root.path("anilist"));
        EpisodeResult episodeResult = upsertEpisode(animeResult.anime(), root, episodeNumber, seekVideoId);

        return new WorkerReleaseResponse(
            true,
            episodeResult.created() ? "Release publicada no Nekoflow." : "Release atualizada no Nekoflow.",
            animeResult.anime().getId().toString(),
            episodeResult.episode().getId().toString(),
            animeResult.created(),
            episodeResult.created()
        );
    }

    private AnimeResult findOrCreateAnime(Long anilistId, String fallbackTitle, JsonNode anilist) {
        JsonNode enrichedAniList = enrichAniList(anilistId, anilist);
        String displayTitle = firstNonBlank(titleFromAniList(enrichedAniList), fallbackTitle);
        String baseSlug = slugify(displayTitle);

        if (anilistId != null) {
            return animeRepository.findByAnilistId(anilistId)
                .map(anime -> new AnimeResult(applyMissingAniListFields(anime, enrichedAniList), false))
                .orElseGet(() -> animeRepository.findBySlugIgnoreCase(baseSlug)
                    .map(anime -> new AnimeResult(applyMissingAniListFields(applyAniListIdIfMissing(anime, anilistId), enrichedAniList), false))
                    .orElseGet(() -> createAnime(anilistId, displayTitle, enrichedAniList)));
        }

        AnimeEntity anime = animeRepository.findBySlugIgnoreCase(baseSlug).orElse(null);
        if (anime != null) {
            return new AnimeResult(applyMissingAniListFields(anime, enrichedAniList), false);
        }
        return createAnime(null, displayTitle, enrichedAniList);
    }

    private AnimeEntity applyAniListIdIfMissing(AnimeEntity anime, Long anilistId) {
        if (anilistId == null || anime.getAnilistId() != null) {
            return anime;
        }
        anime.setAnilistId(anilistId);
        return animeRepository.save(anime);
    }

    private AnimeEntity applyMissingAniListFields(AnimeEntity anime, JsonNode anilist) {
        if (anilist == null || anilist.isMissingNode() || anilist.isNull()) return anime;
        boolean changed = false;
        String cover = firstNonBlank(text(anilist.path("coverImage"), "extraLarge"), text(anilist.path("coverImage"), "large"));
        if (isBlank(anime.getCoverUrl()) && !isBlank(cover)) {
            anime.setCoverUrl(cover);
            changed = true;
        }
        String banner = text(anilist, "bannerImage");
        if (isBlank(anime.getBannerUrl()) && !isBlank(banner)) {
            anime.setBannerUrl(banner);
            changed = true;
        }
        String synopsis = translatedDescription(anilist);
        if (isBlank(anime.getSynopsis()) && !isBlank(synopsis)) {
            anime.setSynopsis(synopsis);
            changed = true;
        }
        String studio = firstStudio(anilist);
        if (isBlank(anime.getStudio()) && !isBlank(studio)) {
            anime.setStudio(studio);
            changed = true;
        }
        Integer year = integerValue(anilist, "seasonYear");
        if (anime.getYear() == null && year != null) {
            anime.setYear(year);
            changed = true;
        }
        String season = buildSeasonLabel(anilist);
        if (isBlank(anime.getSeasonLabel()) && !isBlank(season)) {
            anime.setSeasonLabel(season);
            changed = true;
        }
        List<String> genres = genres(anilist);
        if (anime.getGenres().isEmpty() && !genres.isEmpty()) {
            anime.setGenres(genres);
            changed = true;
        }
        return changed ? animeRepository.save(anime) : anime;
    }

    private AnimeResult createAnime(Long anilistId, String fallbackTitle, JsonNode anilist) {
        AnimeEntity anime = new AnimeEntity();
        anime.setId(UUID.randomUUID());
        anime.setAnilistId(anilistId);
        anime.setTitleRomaji(text(anilist.path("title"), "romaji"));
        anime.setTitleEnglish(text(anilist.path("title"), "english"));
        anime.setTitleNative(text(anilist.path("title"), "native"));
        anime.setTitleDisplay(firstNonBlank(anime.getTitleRomaji(), anime.getTitleEnglish(), fallbackTitle));
        anime.setSlug(uniqueSlug(anime.getTitleDisplay()));
        anime.setSynopsis(translatedDescription(anilist));
        anime.setType(toAnimeType(text(anilist, "format")));
        anime.setStatus(toAnimeStatus(text(anilist, "status")));
        anime.setSeasonLabel(buildSeasonLabel(anilist));
        anime.setYear(integerValue(anilist, "seasonYear"));
        anime.setCoverUrl(firstNonBlank(
            text(anilist.path("coverImage"), "extraLarge"),
            text(anilist.path("coverImage"), "large")
        ));
        anime.setBannerUrl(text(anilist, "bannerImage"));
        anime.setStudio(firstStudio(anilist));
        anime.setGenres(genres(anilist));
        anime.setVisibility(VisibilityStatus.PUBLISHED);
        anime.setPublishedAt(OffsetDateTime.now());
        return new AnimeResult(animeRepository.save(anime), true);
    }

    private EpisodeResult upsertEpisode(AnimeEntity anime, JsonNode root, Integer episodeNumber, String seekVideoId) {
        EpisodeEntity episode = episodeRepository.findByAnimeIdAndNumber(anime.getId(), episodeNumber)
            .orElseGet(() -> {
                EpisodeEntity created = new EpisodeEntity();
                created.setId(UUID.randomUUID());
                created.setAnime(anime);
                created.setNumber(episodeNumber);
                return created;
            });
        boolean created = episode.getPublishedAt() == null && episode.getTitle() == null;

        if (created) {
            episode.setTitle("Episodio " + episodeNumber);
            episode.setSummary(null);
            episode.setDurationSeconds(integerValue(root, "duration_seconds"));
            episode.setThumbnailUrl(text(root, "thumbnail_url"));
            episode.setPreviewUrl(null);
            episode.setStatus(EpisodeStatus.PUBLISHED);
            episode.setScheduledFor(null);
            episode.setPublishedAt(OffsetDateTime.now());
        } else {
            Integer duration = integerValue(root, "duration_seconds");
            String thumbnail = text(root, "thumbnail_url");
            if (episode.getDurationSeconds() == null && duration != null) {
                episode.setDurationSeconds(duration);
            }
            if (isBlank(episode.getThumbnailUrl()) && !isBlank(thumbnail)) {
                episode.setThumbnailUrl(thumbnail);
            }
            if (episode.getStatus() == null) {
                episode.setStatus(EpisodeStatus.PUBLISHED);
            }
            if (episode.getPublishedAt() == null && episode.getStatus() == EpisodeStatus.PUBLISHED) {
                episode.setPublishedAt(OffsetDateTime.now());
            }
        }

        EpisodeEntity saved = episodeRepository.save(episode);
        replaceVideoSource(saved, seekVideoId);
        return new EpisodeResult(saved, created);
    }

    private void replaceVideoSource(EpisodeEntity episode, String seekVideoId) {
        if (isBlank(seekVideoId)) {
            return;
        }

        episodeVideoSourceRepository.deleteByEpisode(episode);
        episode.getVideoSources().clear();

        EpisodeVideoSourceEntity source = new EpisodeVideoSourceEntity();
        source.setId(UUID.randomUUID());
        source.setEpisode(episode);
        source.setProvider(VideoProvider.SEEKSTREAMING);
        source.setExternalVideoId(seekVideoId);
        source.setEmbedUrl(SEEK_EMBED_BASE + seekVideoId);
        source.setPlayerUrl(null);
        source.setDefault(true);
        source.setStatus(VideoSourceStatus.ACTIVE);
        episode.getVideoSources().add(source);
    }

    private void verifySignature(String rawBody, HttpHeaders headers) {
        String secret = appProperties.worker() == null ? null : appProperties.worker().webhookSecret();
        if (isBlank(secret)) {
            return;
        }

        String received = headers.getFirst("X-Nekoflow-Signature");
        if (isBlank(received) || !received.startsWith("sha256=")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Assinatura do worker ausente.");
        }

        String expected = "sha256=" + hmacSha256(secret, rawBody);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Assinatura do worker invalida.");
        }
    }

    private String hmacSha256(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao validar assinatura.", exception);
        }
    }

    private JsonNode parseBody(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON invalido.", exception);
        }
    }

    private AnimeType toAnimeType(String format) {
        if ("MOVIE".equalsIgnoreCase(format)) return AnimeType.MOVIE;
        if ("OVA".equalsIgnoreCase(format)) return AnimeType.OVA;
        if ("SPECIAL".equalsIgnoreCase(format)) return AnimeType.SPECIAL;
        return AnimeType.SERIES;
    }

    private AnimeStatus toAnimeStatus(String status) {
        if ("FINISHED".equalsIgnoreCase(status)) return AnimeStatus.FINISHED;
        if ("HIATUS".equalsIgnoreCase(status)) return AnimeStatus.HIATUS;
        return AnimeStatus.RELEASING;
    }

    private JsonNode enrichAniList(Long anilistId, JsonNode current) {
        if (anilistId == null) return current;
        if (current != null && !current.isMissingNode() && !current.isNull()
            && !isBlank(text(current.path("coverImage"), "extraLarge"))) {
            return current;
        }

        String query = """
            query ($id: Int) {
              Media(id: $id, type: ANIME) {
                id
                title { romaji english native }
                format
                status
                episodes
                seasonYear
                season
                coverImage { extraLarge large }
                bannerImage
                description(asHtml: false)
                genres
                studios(isMain: true) { nodes { name isAnimationStudio } }
              }
            }
            """;
        try {
            JsonNode response = restTemplate.postForObject(
                resolveAniListEndpoint(),
                java.util.Map.of("query", query, "variables", java.util.Map.of("id", anilistId)),
                JsonNode.class
            );
            JsonNode media = response == null ? null : response.path("data").path("Media");
            return media == null || media.isMissingNode() || media.isNull() ? current : media;
        } catch (Exception ignored) {
            return current;
        }
    }

    private String resolveAniListEndpoint() {
        AppProperties.Integrations integrations = appProperties.integrations();
        if (integrations != null && integrations.anilist() != null && !isBlank(integrations.anilist().endpoint())) {
            return integrations.anilist().endpoint();
        }
        String envValue = System.getenv("APP_ANILIST_ENDPOINT");
        return isBlank(envValue) ? "https://graphql.anilist.co" : envValue;
    }

    private String firstStudio(JsonNode anilist) {
        JsonNode nodes = anilist.path("studios").path("nodes");
        if (!nodes.isArray()) return null;
        for (JsonNode node : nodes) {
            if (node.path("isAnimationStudio").asBoolean(false) && !isBlank(text(node, "name"))) {
                return text(node, "name");
            }
        }
        return null;
    }

    private String buildSeasonLabel(JsonNode anilist) {
        String season = text(anilist, "season");
        Integer year = integerValue(anilist, "seasonYear");
        if (isBlank(season) && year == null) return null;
        if (isBlank(season)) return String.valueOf(year);
        return year == null ? season : season + " " + year;
    }

    private List<String> genres(JsonNode anilist) {
        JsonNode nodes = anilist.path("genres");
        if (!nodes.isArray()) return List.of();
        List<String> genres = new ArrayList<>();
        for (JsonNode node : nodes) {
            String genre = node.asText(null);
            if (!isBlank(genre)) {
                genres.add(genre.trim());
            }
        }
        return genres;
    }

    private String uniqueSlug(String title) {
        String base = slugify(title);
        String candidate = base;
        int index = 2;
        while (animeRepository.findBySlugIgnoreCase(candidate).isPresent()) {
            candidate = base + "-" + index;
            index++;
        }
        return candidate;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value == null ? "anime" : value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "anime" : normalized;
    }

    private String titleFromAniList(JsonNode anilist) {
        JsonNode title = anilist.path("title");
        return firstNonBlank(text(title, "romaji"), text(title, "english"), text(title, "native"));
    }

    private String translatedDescription(JsonNode anilist) {
        String description = text(anilist, "description");
        if (isBlank(description)) {
            return null;
        }
        String cleaned = description.replaceAll("<[^>]+>", "").replace("&nbsp;", " ").trim();
        if (isBlank(cleaned)) {
            return null;
        }
        return translateToPortuguese(cleaned);
    }

    private String translateToPortuguese(String text) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=pt&dt=t&q=" + encoded;
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            JsonNode sentences = response == null ? null : response.path(0);
            if (sentences == null || !sentences.isArray()) {
                return text;
            }
            StringBuilder translated = new StringBuilder();
            for (JsonNode sentence : sentences) {
                String part = sentence.path(0).asText(null);
                if (!isBlank(part)) {
                    translated.append(part);
                }
            }
            String result = translated.toString().trim();
            return isBlank(result) ? text : result;
        } catch (Exception ignored) {
            return text;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) return value.trim();
        }
        return null;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.isNull()) return null;
        String text = value.asText(null);
        return isBlank(text) ? null : text.trim();
    }

    private Integer integerValue(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.isNull()) return null;
        if (value.isNumber()) return value.intValue();
        try {
            return Integer.parseInt(value.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long longValue(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.isNull()) return null;
        if (value.isNumber()) return value.longValue();
        try {
            return Long.parseLong(value.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record AnimeResult(AnimeEntity anime, boolean created) {
    }

    private record EpisodeResult(EpisodeEntity episode, boolean created) {
    }
}
