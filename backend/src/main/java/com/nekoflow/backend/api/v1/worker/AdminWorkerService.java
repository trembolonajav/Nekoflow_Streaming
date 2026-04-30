package com.nekoflow.backend.api.v1.worker;

import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekoflow.backend.config.AppProperties;

@Service
public class AdminWorkerService {

    private static final Pattern RE_FANSUB = Pattern.compile("^\\[([^]]+)]\\s+(.+?)\\s+-\\s+(\\d{1,3})(?:v\\d+)?\\s*[\\[(]", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_SXX = Pattern.compile("^(.+?)\\s+S(\\d{1,2})E(\\d{1,3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_SIMPLE = Pattern.compile("^(.+?)[\\s\\-_.]+(\\d{1,3})\\s*\\.[a-z0-9]+$", Pattern.CASE_INSENSITIVE);
    private static final String SEEK_BASE = "https://seekstreaming.com";
    private static final int SEEK_PAGE_SIZE = 100;

    private final JdbcTemplate jdbc;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final WorkerReleaseWebhookService webhookService;

    public AdminWorkerService(
        JdbcTemplate jdbc,
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        AppProperties appProperties,
        WorkerReleaseWebhookService webhookService
    ) {
        this.jdbc = jdbc;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.webhookService = webhookService;
    }

    public Map<String, Object> dashboard() {
        return Map.of(
            "seek_videos", count("select count(*) from seek_videos"),
            "queue_pending", count("select count(*) from release_queue where status in ('pending','matched','needs_review','anilist_unavailable','approved','publish_failed')"),
            "queue_published", count("select count(*) from release_queue where status = 'published'"),
            "queue_error", count("select count(*) from release_queue where status in ('error','publish_failed')")
        );
    }

    public Map<String, Object> pingSeek() {
        long started = System.currentTimeMillis();
        try {
            Map<String, Object> body = seekExchange("/api/v1/user/information", new ParameterizedTypeReference<>() {});
            return Map.of("ok", true, "latency_ms", System.currentTimeMillis() - started, "status", 200, "user", body);
        } catch (Exception exception) {
            return Map.of("ok", false, "latency_ms", System.currentTimeMillis() - started, "error", exception.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> syncSeek() {
        long started = System.currentTimeMillis();
        int page = 1;
        int fetched = 0;
        int upserted = 0;
        int total = 0;
        List<String> errors = new ArrayList<>();

        while (page <= 200) {
            try {
                URI uri = URI.create(SEEK_BASE + "/api/v1/video/manage?page=" + page + "&perPage=" + SEEK_PAGE_SIZE);
                Map<String, Object> response = seekExchange(uri.toString(), new ParameterizedTypeReference<>() {}, true);
                Map<?, ?> metadata = response.get("metadata") instanceof Map<?, ?> m ? m : Map.of();
                List<?> data = response.get("data") instanceof List<?> list ? list : List.of();
                total = intValue(metadata.get("total"), total);
                fetched += data.size();
                for (Object item : data) {
                    if (item instanceof Map<?, ?> video) {
                        upsertSeekVideo(video);
                        upserted++;
                    }
                }
                int maxPage = intValue(metadata.get("maxPage"), page);
                if (page >= maxPage || data.isEmpty()) break;
                page++;
            } catch (Exception exception) {
                errors.add("page " + page + ": " + exception.getMessage());
                break;
            }
        }

        return Map.of(
            "ok", errors.isEmpty() || upserted > 0,
            "latency_ms", System.currentTimeMillis() - started,
            "total_in_seek", total,
            "fetched", fetched,
            "upserted", upserted,
            "pages_scanned", page,
            "errors", errors.size(),
            "error_details", errors
        );
    }

    public Map<String, Object> seekVideos(String search, int page, int size) {
        String term = "%" + nullToBlank(search).trim().toLowerCase() + "%";
        List<Map<String, Object>> rows = jdbc.queryForList("""
            select seek_video_id, filename, duration_seconds, size_bytes, seek_status, thumbnail_url, last_synced_at
            from seek_videos
            where lower(coalesce(filename, '')) like ?
            order by filename asc
            limit ? offset ?
            """, term, size, Math.max(page, 0) * size);
        return Map.of(
            "items", rows,
            "total", count("select count(*) from seek_videos"),
            "active", count("select count(*) from seek_videos where seek_status = 'Active'")
        );
    }

    public Map<String, Object> importOptions(String search, String status) {
        String term = "%" + nullToBlank(search).trim().toLowerCase() + "%";
        List<Map<String, Object>> rows = jdbc.queryForList("""
            select v.seek_video_id, v.filename, v.duration_seconds, v.size_bytes, v.seek_status, v.thumbnail_url, q.status as queue_status
            from seek_videos v
            left join release_queue q on q.seek_video_id = v.seek_video_id
            where lower(coalesce(v.filename, '')) like ?
              and (? = 'all' or (? = 'missing' and q.id is null) or (? = 'imported' and q.id is not null))
            order by v.filename asc
            limit 2000
            """, term, status, status, status);
        return Map.of(
            "items", rows,
            "total", count("select count(*) from seek_videos"),
            "imported", count("select count(*) from release_queue where seek_video_id is not null"),
            "missing", count("select count(*) from seek_videos v left join release_queue q on q.seek_video_id = v.seek_video_id where q.id is null")
        );
    }

    @Transactional
    public Map<String, Object> importSelected(Map<String, Object> body) {
        List<String> ids = stringList(body.get("ids"));
        return parseVideos(ids, true, Math.min(Math.max(ids.size(), 1), 500));
    }

    public Map<String, Object> queue(String status, String search, int page, int size) {
        String term = "%" + nullToBlank(search).trim().toLowerCase() + "%";
        List<Map<String, Object>> rows = jdbc.queryForList("""
            select id, seek_video_id, source, parsed_title, parsed_season, parsed_episode, anilist_id,
                   anilist_payload, match_confidence, matched_anime_id, thumbnail_url, duration_seconds,
                   status, status_reason, updated_at
            from release_queue
            where (? = 'all' or (? = 'active' and status <> 'published') or status = ?)
              and lower(coalesce(parsed_title, '')) like ?
            order by updated_at desc
            limit ? offset ?
            """, status, status, status, term, size, Math.max(page, 0) * size);
        return Map.of("items", rows, "counts", queueCounts());
    }

    @Transactional
    public Map<String, Object> parseQueue(Map<String, Object> body) {
        boolean reprocess = Boolean.TRUE.equals(body.get("reprocess"));
        return parseVideos(List.of(), reprocess, 100);
    }

    @Transactional
    public Map<String, Object> approve(String id) {
        jdbc.update("update release_queue set status = 'approved', approved_at = now(), approved_by = 'local-admin', updated_at = now() where id = ?::uuid", id);
        return Map.of("ok", true);
    }

    @Transactional
    public Map<String, Object> approveMany(Map<String, Object> body) {
        List<String> ids = stringList(body.get("ids"));
        int approved = 0;
        for (String id : ids) {
            approved += jdbc.update("update release_queue set status = 'approved', approved_at = now(), approved_by = 'local-admin', updated_at = now() where id = ?::uuid", id);
        }
        return Map.of("ok", true, "approved", approved, "total", ids.size());
    }

    @Transactional
    public Map<String, Object> updateQueueItem(String id, Map<String, Object> body) {
        jdbc.update("""
            update release_queue
            set parsed_title = ?,
                parsed_season = ?,
                parsed_episode = ?,
                anilist_id = ?,
                matched_anime_id = ?,
                match_confidence = ?,
                thumbnail_url = ?,
                duration_seconds = ?,
                status = ?,
                status_reason = ?,
                updated_at = now()
            where id = ?::uuid
            """,
            text(body, "parsed_title"),
            integer(body.get("parsed_season")),
            integer(body.get("parsed_episode")),
            integer(body.get("anilist_id")),
            text(body, "matched_anime_id"),
            decimal(body.get("match_confidence")),
            text(body, "thumbnail_url"),
            integer(body.get("duration_seconds")),
            firstNonBlank(text(body, "status"), "needs_review"),
            text(body, "status_reason"),
            id
        );
        return Map.of("ok", true, "item", findQueueRow(id));
    }

    @Transactional
    public Map<String, Object> publishMany(Map<String, Object> body) {
        List<String> ids = stringList(body.get("ids"));
        if (ids.isEmpty()) {
            ids = jdbc.queryForList("select id::text from release_queue where status = 'approved' order by updated_at desc limit 100", String.class);
        }
        return publish(ids);
    }

    @Transactional
    public Map<String, Object> publish(List<String> ids) {
        int published = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        for (String id : ids) {
            Map<String, Object> row = findQueueRow(id);
            Map<String, Object> payloadMap = new LinkedHashMap<>();
            payloadMap.put("event", "release.publish");
            payloadMap.put("release_queue_id", row.get("id"));
            payloadMap.put("source", row.get("source"));
            payloadMap.put("seek_video_id", row.get("seek_video_id"));
            payloadMap.put("title", row.get("parsed_title"));
            payloadMap.put("season", row.get("parsed_season"));
            payloadMap.put("episode", row.get("parsed_episode"));
            payloadMap.put("anilist_id", row.get("anilist_id"));
            payloadMap.put("matched_anime_id", row.get("matched_anime_id"));
            payloadMap.put("match_confidence", row.get("match_confidence"));
            payloadMap.put("thumbnail_url", row.get("thumbnail_url"));
            payloadMap.put("duration_seconds", row.get("duration_seconds"));
            payloadMap.put("anilist", parseJson(row.get("anilist_payload")));
            payloadMap.put("sent_at", OffsetDateTime.now().toString());
            String payload = toJson(payloadMap);
            try {
                var response = webhookService.publishRelease(payload, new HttpHeaders());
                jdbc.update("""
                    update release_queue set status = 'published', status_reason = null, published_at = now(), updated_at = now()
                    where id = ?::uuid
                    """, id);
                logWebhook(id, "/api/v1/worker/webhooks/releases", payload, 200, toJson(response), null);
                published++;
                results.add(Map.of("id", id, "ok", true));
            } catch (Exception exception) {
                jdbc.update("update release_queue set status = 'publish_failed', status_reason = ?, updated_at = now() where id = ?::uuid", exception.getMessage(), id);
                logWebhook(id, "/api/v1/worker/webhooks/releases", payload, 500, null, exception.getMessage());
                results.add(Map.of("id", id, "ok", false, "error", exception.getMessage()));
            }
        }
        return Map.of("ok", true, "total", ids.size(), "published", published, "failed", ids.size() - published, "results", results);
    }

    public Map<String, Object> logs() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            select l.id, l.release_queue_id, l.endpoint, l.response_status, l.response_body, l.error, l.attempt, l.created_at,
                   q.parsed_title, q.parsed_episode
            from webhook_log l
            left join release_queue q on q.id = l.release_queue_id
            order by l.created_at desc
            limit 50
            """);
        int total = count("select count(*) from webhook_log");
        int ok = count("select count(*) from webhook_log where response_status >= 200 and response_status < 300");
        return Map.of("items", rows, "total", total, "ok", ok, "fail", total - ok);
    }

    public List<Map<String, Object>> sources() {
        return jdbc.queryForList("select * from rss_sources order by created_at desc");
    }

    @Transactional
    public Map<String, Object> createSource(Map<String, Object> body) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            insert into rss_sources (id, name, url, release_group_filter, quality_filter, enabled)
            values (?, ?, ?, ?, ?, ?)
            """, id, text(body, "name"), text(body, "url"), text(body, "release_group_filter"), text(body, "quality_filter"), bool(body.get("enabled"), true));
        return Map.of("ok", true, "id", id.toString());
    }

    @Transactional
    public Map<String, Object> updateSource(String id, Map<String, Object> body) {
        jdbc.update("""
            update rss_sources
            set name = ?, url = ?, release_group_filter = ?, quality_filter = ?, enabled = ?, updated_at = now()
            where id = ?::uuid
            """, text(body, "name"), text(body, "url"), text(body, "release_group_filter"), text(body, "quality_filter"), bool(body.get("enabled"), true), id);
        return Map.of("ok", true);
    }

    @Transactional
    public void deleteSource(String id) {
        jdbc.update("delete from rss_sources where id = ?::uuid", id);
    }

    @Transactional
    public Map<String, Object> pollSources(Map<String, Object> body) {
        List<Map<String, Object>> sources = body.get("sourceId") == null
            ? jdbc.queryForList("select * from rss_sources where enabled = true")
            : jdbc.queryForList("select * from rss_sources where enabled = true and id = ?::uuid", String.valueOf(body.get("sourceId")));
        List<Map<String, Object>> summary = new ArrayList<>();
        for (Map<String, Object> source : sources) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("source_id", source.get("id"));
            item.put("name", source.get("name"));
            item.put("fetched", 0);
            item.put("new", 0);
            item.put("filtered", 0);
            item.put("error", "Poll RSS local ainda nao implementa download do feed; fonte salva para proxima etapa.");
            jdbc.update("update rss_sources set last_polled_at = now(), last_poll_error = ? where id = ?", item.get("error"), source.get("id"));
            summary.add(item);
        }
        return Map.of("ok", true, "sources", sources.size(), "summary", summary);
    }

    private Map<String, Object> parseVideos(List<String> onlyIds, boolean reprocess, int limit) {
        long started = System.currentTimeMillis();
        List<Map<String, Object>> videos;
        if (onlyIds.isEmpty()) {
            videos = jdbc.queryForList("""
                select v.seek_video_id, v.filename, v.title, v.thumbnail_url, v.duration_seconds
                from seek_videos v
                left join release_queue q on q.seek_video_id = v.seek_video_id
                where q.id is null or q.status in ('anilist_unavailable','needs_review','pending')
                order by case q.status when 'anilist_unavailable' then 0 when 'pending' then 1 when 'needs_review' then 2 else 3 end,
                         v.last_synced_at desc
                limit ?
                """, limit);
        } else {
            String placeholders = String.join(",", onlyIds.stream().map(id -> "?").toList());
            List<Object> args = new ArrayList<>(onlyIds);
            args.add(limit);
            videos = jdbc.queryForList("""
                select seek_video_id, filename, title, thumbnail_url, duration_seconds
                from seek_videos
                where seek_video_id in (%s)
                limit ?
                """.formatted(placeholders), args.toArray());
        }

        int scanned = 0, parsedCount = 0, parseFailed = 0, matched = 0, review = 0, unavailable = 0, errors = 0;
        Map<String, AniListMatch> aniListCache = new LinkedHashMap<>();
        for (Map<String, Object> video : videos) {
            scanned++;
            ParsedRelease parsed = parseFilename(firstNonBlank((String) video.get("filename"), (String) video.get("title")));
            if (parsed == null) {
                parseFailed++;
                upsertQueue(video, null, null, null, BigDecimal.ZERO, "needs_review", "filename nao reconhecido pelo parser");
                continue;
            }
            parsedCount++;
            AniListMatch match = resolveAniListMatch(parsed.title(), aniListCache);
            if (match.unavailable()) {
                unavailable++;
                upsertQueue(video, parsed, null, null, BigDecimal.ZERO, "anilist_unavailable", match.error() == null ? "AniList indisponivel/rate limit" : "AniList indisponivel/rate limit: " + truncate(match.error(), 180));
            } else if (match.media() != null) {
                BigDecimal confidence = confidence(parsed.title(), match.media());
                boolean isMatched = confidence.compareTo(new BigDecimal("0.800")) >= 0;
                if (isMatched) matched++; else review++;
                upsertQueue(video, parsed, match.media().path("id").asInt(), match.media(), confidence, isMatched ? "matched" : "needs_review", isMatched ? null : "match fraco");
            } else {
                review++;
                upsertQueue(video, parsed, null, null, BigDecimal.ZERO, "needs_review", match.error() == null ? "sem match no AniList" : match.error());
                if (match.error() != null) errors++;
            }
        }

        return Map.of(
            "ok", true,
            "latency_ms", System.currentTimeMillis() - started,
            "scanned", scanned,
            "parsed", parsedCount,
            "parse_failed", parseFailed,
            "matched", matched,
            "needs_review", review,
            "anilist_unavailable", unavailable,
            "errors", errors
        );
    }

    private void upsertSeekVideo(Map<?, ?> video) {
        String id = string(video.get("id"));
        if (id == null) return;
        jdbc.update("""
            insert into seek_videos (seek_video_id, filename, title, thumbnail_url, duration_seconds, size_bytes, seek_status, raw_payload, last_synced_at)
            values (?, ?, ?, ?, ?, ?, ?, ?::jsonb, now())
            on conflict (seek_video_id) do update set
              filename = excluded.filename,
              title = excluded.title,
              thumbnail_url = excluded.thumbnail_url,
              duration_seconds = excluded.duration_seconds,
              size_bytes = excluded.size_bytes,
              seek_status = excluded.seek_status,
              raw_payload = excluded.raw_payload,
              last_synced_at = now(),
              updated_at = now()
            """,
            id,
            string(video.get("name")),
            string(video.get("name")),
            string(video.get("poster")),
            integer(video.get("duration")),
            longNumber(video.get("size")),
            string(video.get("status")),
            toJson(video)
        );
    }

    private void upsertQueue(Map<String, Object> video, ParsedRelease parsed, Integer anilistId, JsonNode media, BigDecimal confidence, String status, String reason) {
        String title = parsed == null ? null : parsed.title();
        Integer season = parsed == null ? null : parsed.season();
        Integer episode = parsed == null ? null : parsed.episode();
        String thumb = media == null
            ? (String) video.get("thumbnail_url")
            : firstNonBlank(text(media.path("coverImage"), "extraLarge"), text(media.path("coverImage"), "large"), (String) video.get("thumbnail_url"));
        jdbc.update("""
            insert into release_queue (seek_video_id, source, parsed_title, parsed_season, parsed_episode, anilist_id, anilist_payload, match_confidence, matched_anime_id, thumbnail_url, duration_seconds, status, status_reason)
            values (?, 'seek', ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?)
            on conflict (seek_video_id) do update set
              parsed_title = excluded.parsed_title,
              parsed_season = excluded.parsed_season,
              parsed_episode = excluded.parsed_episode,
              anilist_id = excluded.anilist_id,
              anilist_payload = excluded.anilist_payload,
              match_confidence = excluded.match_confidence,
              matched_anime_id = excluded.matched_anime_id,
              thumbnail_url = excluded.thumbnail_url,
              duration_seconds = excluded.duration_seconds,
              status = case when release_queue.status = 'published' then release_queue.status else excluded.status end,
              status_reason = case when release_queue.status = 'published' then release_queue.status_reason else excluded.status_reason end,
              updated_at = now()
            """,
            video.get("seek_video_id"), title, season, episode, anilistId, media == null ? null : media.toString(),
            confidence, anilistId == null ? null : String.valueOf(anilistId), thumb, video.get("duration_seconds"), status, reason);
    }

    private AniListMatch resolveAniListMatch(String title, Map<String, AniListMatch> memoryCache) {
        String cacheKey = normalize(title);
        AniListMatch memoryMatch = memoryCache.get(cacheKey);
        if (memoryMatch != null) return memoryMatch;

        AniListMatch dbMatch = readAniListCache(cacheKey);
        if (dbMatch != null) {
            memoryCache.put(cacheKey, dbMatch);
            return dbMatch;
        }

        AniListMatch match = searchAniList(title);
        if (match.unavailable()) {
            sleepQuietly(10_000);
            match = searchAniList(title);
        }
        if (!match.unavailable()) {
            writeAniListCache(cacheKey, match.media());
            memoryCache.put(cacheKey, match);
        }
        sleepQuietly(1_500);
        return match;
    }

    private AniListMatch readAniListCache(String cacheKey) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            select payload
            from anilist_cache
            where search_key = ? and expires_at > now()
            limit 1
            """, cacheKey);
        if (rows.isEmpty()) return null;
        jdbc.update("update anilist_cache set hit_count = hit_count + 1, last_hit_at = now(), updated_at = now() where search_key = ?", cacheKey);
        Object payload = rows.get(0).get("payload");
        if (payload == null) return new AniListMatch(null, false, null);
        try {
            return new AniListMatch(objectMapper.readTree(String.valueOf(payload)), false, null);
        } catch (Exception exception) {
            return null;
        }
    }

    private void writeAniListCache(String cacheKey, JsonNode media) {
        Integer anilistId = media == null ? null : media.path("id").asInt();
        jdbc.update("""
            insert into anilist_cache (search_key, anilist_id, payload, hit_count, last_hit_at, expires_at)
            values (?, ?, ?::jsonb, 1, now(), now() + interval '30 days')
            on conflict (search_key) do update set
              anilist_id = excluded.anilist_id,
              payload = excluded.payload,
              hit_count = anilist_cache.hit_count + 1,
              last_hit_at = now(),
              updated_at = now(),
              expires_at = excluded.expires_at
            """, cacheKey, anilistId, media == null ? null : media.toString());
    }

    private AniListMatch searchAniList(String title) {
        try {
            String query = "query ($search: String) { Media(search: $search, type: ANIME) { id title { romaji english native } episodes coverImage { large extraLarge } format status season seasonYear bannerImage description(asHtml: false) genres studios(isMain: true) { nodes { name isAnimationStudio } } } }";
            Map<String, Object> body = Map.of("query", query, "variables", Map.of("search", title));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            JsonNode response = restTemplate.postForObject(resolveAniListEndpoint(), new HttpEntity<>(body, headers), JsonNode.class);
            JsonNode media = response == null ? null : response.path("data").path("Media");
            return new AniListMatch(media == null || media.isMissingNode() || media.isNull() ? null : media, false, null);
        } catch (Exception exception) {
            String message = exception.getMessage();
            boolean rateLimitedOrUnavailable = message != null
                && (message.contains("403") || message.contains("429") || message.toLowerCase().contains("too many requests"));
            return new AniListMatch(null, rateLimitedOrUnavailable, message);
        }
    }

    private BigDecimal confidence(String parsedTitle, JsonNode media) {
        String p = normalize(parsedTitle);
        double best = 0;
        for (String candidate : List.of(text(media.path("title"), "romaji"), text(media.path("title"), "english"), text(media.path("title"), "native"))) {
            if (candidate == null) continue;
            String c = normalize(candidate);
            if (c.equals(p)) best = Math.max(best, 1);
            else if (c.contains(p) || p.contains(c)) best = Math.max(best, 0.8);
            else {
                List<String> pw = List.of(p.split(" "));
                List<String> cw = List.of(c.split(" "));
                long overlap = cw.stream().filter(pw::contains).count();
                best = Math.max(best, (overlap / Math.max((double) cw.size(), 1)) * 0.7);
            }
        }
        return BigDecimal.valueOf(Math.round(best * 1000) / 1000.0);
    }

    private ParsedRelease parseFilename(String name) {
        if (name == null || name.isBlank()) return null;
        Matcher m = RE_FANSUB.matcher(name.trim());
        if (m.find()) return new ParsedRelease(cleanTitle(m.group(2)), 1, Integer.parseInt(m.group(3)));
        m = RE_SXX.matcher(name.trim());
        if (m.find()) return new ParsedRelease(cleanTitle(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
        m = RE_SIMPLE.matcher(name.trim());
        if (m.find()) return new ParsedRelease(cleanTitle(m.group(1)), 1, Integer.parseInt(m.group(2)));
        return null;
    }

    private Map<String, Integer> queueCounts() {
        return Map.of(
            "total", count("select count(*) from release_queue"),
            "matched", count("select count(*) from release_queue where status = 'matched'"),
            "review", count("select count(*) from release_queue where status = 'needs_review'"),
            "unavailable", count("select count(*) from release_queue where status = 'anilist_unavailable'"),
            "approved", count("select count(*) from release_queue where status = 'approved'"),
            "published", count("select count(*) from release_queue where status = 'published'"),
            "failed", count("select count(*) from release_queue where status = 'publish_failed'")
        );
    }

    private Map<String, Object> findQueueRow(String id) {
        return jdbc.queryForMap("select * from release_queue where id = ?::uuid", id);
    }

    private void logWebhook(String id, String endpoint, String requestBody, int status, String responseBody, String error) {
        jdbc.update("""
            insert into webhook_log (release_queue_id, endpoint, request_body, response_status, response_body, error)
            values (?::uuid, ?, ?::jsonb, ?, ?, ?)
            """, id, endpoint, requestBody, status, responseBody, error);
    }

    private <T> T seekExchange(String path, ParameterizedTypeReference<T> type) {
        return seekExchange(resolveSeekEndpoint() + path, type, true);
    }

    private <T> T seekExchange(String url, ParameterizedTypeReference<T> type, boolean absolute) {
        String token = resolveSeekToken();
        if (token == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SeekStreaming token ausente.");
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("api-token", token);
        return Objects.requireNonNull(restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), type).getBody());
    }

    private String resolveSeekEndpoint() {
        var integrations = appProperties.integrations();
        if (integrations != null && integrations.seekstreaming() != null && integrations.seekstreaming().endpoint() != null && !integrations.seekstreaming().endpoint().isBlank()) {
            return integrations.seekstreaming().endpoint().replaceAll("/$", "");
        }
        return SEEK_BASE;
    }

    private String resolveSeekToken() {
        var integrations = appProperties.integrations();
        if (integrations != null && integrations.seekstreaming() != null && integrations.seekstreaming().apiToken() != null && !integrations.seekstreaming().apiToken().isBlank()) {
            return integrations.seekstreaming().apiToken();
        }
        String envValue = System.getenv("APP_SEEKSTREAMING_API_TOKEN");
        return envValue == null || envValue.isBlank() ? null : envValue;
    }

    private String resolveAniListEndpoint() {
        var integrations = appProperties.integrations();
        return integrations == null || integrations.anilist() == null || integrations.anilist().endpoint() == null
            ? "https://graphql.anilist.co"
            : integrations.anilist().endpoint();
    }

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private Object parseJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.readValue(String.valueOf(value), Object.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String cleanTitle(String s) {
        return s == null ? null : s.replace('.', ' ').replace('_', ' ').trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return null;
    }

    private String text(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value).trim();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText(null);
    }

    private String string(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    private Integer integer(Object value) {
        return value == null ? null : intValue(value, 0);
    }

    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal number) return number;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        try {
            return value == null || String.valueOf(value).isBlank() ? null : new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long longNumber(Object value) {
        if (value instanceof Number number) return number.longValue();
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private boolean bool(Object value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> raw)) return List.of();
        return raw.stream().map(String::valueOf).toList();
    }

    private record ParsedRelease(String title, Integer season, Integer episode) {
    }

    private record AniListMatch(JsonNode media, boolean unavailable, String error) {
    }
}
