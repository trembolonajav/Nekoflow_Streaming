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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekoflow.backend.config.AppProperties;
import com.nekoflow.backend.security.AppUserPrincipal;

@Service
public class AdminWorkerService {

    private static final Pattern RE_FANSUB = Pattern.compile("^\\[([^]]+)]\\s+(.+?)\\s+-\\s+(\\d{1,3})(?:v\\d+)?\\s*[\\[(]", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_SXX = Pattern.compile("^(.+?)\\s+S(\\d{1,2})E(\\d{1,3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_SIMPLE = Pattern.compile("^(.+?)[\\s\\-_.]+(\\d{1,3})\\s*\\.[a-z0-9]+$", Pattern.CASE_INSENSITIVE);
    private static final String SEEK_BASE = "https://seekstreaming.com";
    private static final int SEEK_PAGE_SIZE = 100;
    private static final int CATALOG_DRAIN_BATCH_SIZE = 3;
    private static final int CATALOG_DRAIN_DELAY_SECONDS = 60;
    private static final int CATALOG_DRAIN_MAX_ATTEMPTS = 200;
    private static final int RSS_POLL_MAX_PER_SOURCE = 50;
    private static final int MAX_CONSECUTIVE_SEEK_FAILURES = 5;
    private static final long CATALOG_MAX_TORRENT_BYTES = 3L * 1024L * 1024L * 1024L;

    private final JdbcTemplate jdbc;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final WorkerReleaseWebhookService webhookService;
    private final ScheduledExecutorService catalogDrainExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "catalog-drain");
        thread.setDaemon(true);
        return thread;
    });

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
        jdbc.update("update release_queue set status = 'approved', approved_at = now(), approved_by = ?, updated_at = now() where id = ?::uuid", currentApprover(), id);
        return Map.of("ok", true);
    }

    @Transactional
    public Map<String, Object> approveMany(Map<String, Object> body) {
        List<String> ids = stringList(body.get("ids"));
        String approver = currentApprover();
        int approved = 0;
        for (String id : ids) {
            approved += jdbc.update("update release_queue set status = 'approved', approved_at = now(), approved_by = ?, updated_at = now() where id = ?::uuid", approver, id);
        }
        return Map.of("ok", true, "approved", approved, "total", ids.size());
    }

    /** Identidade do admin autenticado para trilha de auditoria (aprovacao/publicacao). */
    private String currentApprover() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal.getUsername();
        }
        return "system";
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
            // Idempotencia: nao republica algo ja publicado (evita episodio/log duplicado).
            if ("published".equals(string(row.get("status")))) {
                results.add(Map.of("id", id, "ok", true, "skipped", "already_published"));
                continue;
            }
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
                var response = webhookService.publishReleaseTrusted(payload);
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

    /**
     * Poll RSS real: baixa cada feed habilitado, parseia os itens, aplica filtros
     * de grupo/qualidade, deduplica por seen_releases e reusa o pipeline existente
     * (envia o magnet ao Seek via advance-upload). Depois o sync/parse trazem para
     * a fila. NAO e @Transactional de proposito: fetch de feed + HTTP ao Seek nao
     * devem segurar uma conexao do pool. Cada escrita e auto-commit e idempotente.
     */
    public Map<String, Object> pollSources(Map<String, Object> body) {
        List<Map<String, Object>> sources = body.get("sourceId") == null
            ? jdbc.queryForList("select * from rss_sources where enabled = true")
            : jdbc.queryForList("select * from rss_sources where enabled = true and id = ?::uuid", String.valueOf(body.get("sourceId")));

        if (sources.isEmpty()) {
            return Map.of("ok", true, "sources", 0, "new", 0, "summary", List.of());
        }

        String folderId = string(findUploadedFolder().get("id"));
        List<Map<String, Object>> summary = new ArrayList<>();
        int totalNew = 0;
        for (Map<String, Object> source : sources) {
            Map<String, Object> result = pollSingleSource(source, folderId);
            totalNew += (int) result.getOrDefault("new", 0);
            summary.add(result);
        }
        return Map.of("ok", true, "sources", sources.size(), "new", totalNew, "summary", summary);
    }

    private Map<String, Object> pollSingleSource(Map<String, Object> source, String folderId) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("source_id", source.get("id"));
        item.put("name", source.get("name"));
        int fetched = 0;
        int filtered = 0;
        int duplicate = 0;
        int added = 0;
        int failed = 0;
        int consecutiveFailures = 0;
        String error = null;
        try {
            String group = string(source.get("release_group_filter"));
            List<String> qualityTokens = filterTokens(string(source.get("quality_filter")));
            List<RssFeedParser.RssItem> items = RssFeedParser.parse(fetchText(string(source.get("url"))));
            fetched = items.size();
            for (RssFeedParser.RssItem rss : items) {
                if (added >= RSS_POLL_MAX_PER_SOURCE) break;
                if (rss.infohash() == null || !matchesGroup(rss.title(), group) || !matchesTokens(rss.title(), qualityTokens)) {
                    filtered++;
                    continue;
                }
                if (seenInfohash(rss.infohash())) {
                    duplicate++;
                    continue;
                }
                String magnet = rss.magnet() != null ? rss.magnet() : "magnet:?xt=urn:btih:" + rss.infohash();
                try {
                    Map<String, Object> task = createSeekAdvancedUpload(new NyaaItem(rss.title(), rss.infohash(), magnet, null, "", 0L), folderId);
                    jdbc.update("""
                        insert into seen_releases (infohash, guid, source_id, release_name)
                        values (?, ?, ?::uuid, ?)
                        on conflict (infohash) do nothing
                        """, rss.infohash(), firstNonBlank(string(task.get("id")), rss.guid()), string(source.get("id")), rss.title());
                    added++;
                    consecutiveFailures = 0;
                    sleepQuietly(200);
                } catch (Exception uploadError) {
                    failed++;
                    consecutiveFailures++;
                    error = "ingest: " + truncate(uploadError.getMessage(), 160);
                    // Aborta a fonte se o Seek estiver falhando em sequencia (evita
                    // martelar um endpoint quebrado); as demais fontes seguem.
                    if (consecutiveFailures >= MAX_CONSECUTIVE_SEEK_FAILURES) {
                        error = "ingest abortado apos " + consecutiveFailures + " falhas: " + error;
                        break;
                    }
                }
            }
        } catch (Exception fetchError) {
            error = "feed: " + truncate(fetchError.getMessage(), 180);
        }
        item.put("fetched", fetched);
        item.put("new", added);
        item.put("filtered", filtered);
        item.put("duplicate", duplicate);
        item.put("failed", failed);
        if (error != null) {
            item.put("error", error);
        }
        jdbc.update("update rss_sources set last_polled_at = now(), last_poll_items = ?, last_poll_error = ? where id = ?",
            added, error, source.get("id"));
        return item;
    }

    public Map<String, Object> catalogJobs() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            select id, url, source, status, status_reason, pages_total, pages_done,
                   items_found, items_new, items_filtered, items_failed,
                   release_group_filter, quality_filter, started_at, finished_at, created_at, updated_at
            from crawl_jobs
            order by created_at desc
            limit 100
            """);
        return Map.of("items", rows, "total", rows.size());
    }

    @Transactional
    public Map<String, Object> createCatalogJob(Map<String, Object> body) {
        String url = firstNonBlank(text(body, "url"), "https://nyaa.si/?f=0&c=1_2&q=");
        UUID id = UUID.randomUUID();
        jdbc.update("""
            insert into crawl_jobs (id, url, source, release_group_filter, quality_filter)
            values (?, ?, 'nyaa', ?, ?)
            """, id, url, text(body, "release_group_filter"), text(body, "quality_filter"));
        return Map.of("ok", true, "id", id.toString());
    }

    @Transactional
    public Map<String, Object> runCatalogJob(String id) {
        Map<String, Object> job = jdbc.queryForMap("select * from crawl_jobs where id = ?::uuid", id);
        jdbc.update("""
            update crawl_jobs
            set status = 'running', status_reason = null, started_at = now(), pages_done = 0,
                items_found = 0, items_new = 0, items_filtered = 0, items_failed = 0, updated_at = now()
            where id = ?::uuid
            """, id);

        int pagesDone = 0;
        int pagesTotal = 1;
        int itemsFound = 0;
        int itemsNew = 0;
        int itemsFiltered = 0;
        int itemsFailed = 0;
        int duplicateItems = 0;
        try {
            String firstPage = fetchText(nyaaPageUrl(String.valueOf(job.get("url")), 1));
            pagesTotal = Math.min(Math.max(detectNyaaPages(firstPage), 1), 10);
            List<NyaaItem> allItems = new ArrayList<>(parseNyaaItems(firstPage));
            int firstPageLength = firstPage == null ? 0 : firstPage.length();
            int firstPageMagnets = countMatches(firstPage, "magnet:?xt=urn:btih");
            pagesDone = 1;
            for (int page = 2; page <= pagesTotal; page++) {
                String html = fetchText(nyaaPageUrl(String.valueOf(job.get("url")), page));
                List<NyaaItem> pageItems = parseNyaaItems(html);
                if (pageItems.isEmpty()) break;
                allItems.addAll(pageItems);
                pagesDone = page;
            }
            itemsFound = allItems.size();

            List<NyaaItem> selected = selectCatalogItems(allItems, string(job.get("release_group_filter")), string(job.get("quality_filter")));
            itemsFiltered = Math.max(itemsFound - selected.size(), 0);
            List<NyaaItem> newItems = new ArrayList<>();
            for (NyaaItem item : selected) {
                if (item.infohash() != null && seenInfohash(item.infohash())) duplicateItems++;
                else newItems.add(item);
            }

            Map<String, Object> uploadFolder = findUploadedFolder();
            String folderId = string(uploadFolder.get("id"));
            itemsFailed = newItems.size();
            String reason = "found=%d selected=%d duplicates=%d sent=%d failed=%d folder=%s drain=scheduled".formatted(
                itemsFound,
                selected.size(),
                duplicateItems,
                itemsNew,
                itemsFailed,
                folderId
            );
            if (itemsFound == 0) {
                reason = reason + " firstPageLength=" + firstPageLength + " firstPageMagnets=" + firstPageMagnets;
            }
            jdbc.update("""
                update crawl_jobs
                set status = ?, status_reason = ?, pages_total = ?, pages_done = ?, items_found = ?, items_new = ?,
                    items_filtered = ?, items_failed = ?, finished_at = ?, updated_at = now()
                where id = ?::uuid
                """,
                newItems.isEmpty() ? "done" : "ingesting",
                reason,
                pagesTotal,
                pagesDone,
                itemsFound,
                itemsNew,
                itemsFiltered,
                itemsFailed,
                newItems.isEmpty() ? OffsetDateTime.now() : null,
                id
            );
            if (!newItems.isEmpty()) {
                scheduleCatalogDrain(id, newItems, folderId, itemsFound, selected.size(), duplicateItems, 1);
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", true);
            response.put("jobId", id);
            response.put("pages", pagesDone);
            response.put("pagesTotal", pagesTotal);
            response.put("itemsFound", itemsFound);
            response.put("itemsSelected", selected.size());
            response.put("itemsNew", itemsNew);
            response.put("itemsFiltered", itemsFiltered);
            response.put("itemsFailed", itemsFailed);
            response.put("duplicates", duplicateItems);
            response.put("drainScheduled", !newItems.isEmpty());
            return response;
        } catch (Exception exception) {
            String message = truncate(exception.getMessage(), 500);
            jdbc.update("""
                update crawl_jobs
                set status = 'failed', status_reason = ?, pages_done = 0, items_found = ?, items_new = ?,
                    items_filtered = ?, items_failed = ?, finished_at = now(), updated_at = now()
                where id = ?::uuid
                """, message, itemsFound, itemsNew, itemsFiltered, itemsFailed, id);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message, exception);
        }
    }

    @Transactional
    public Map<String, Object> cancelCatalogJob(String id) {
        int updated = jdbc.update("""
            update crawl_jobs
            set status = 'cancelled', status_reason = 'cancelado pelo admin',
                finished_at = now(), updated_at = now()
            where id = ?::uuid and status in ('pending', 'running', 'ingesting')
            """, id);
        return Map.of("ok", true, "cancelled", updated);
    }

    @Transactional
    public void deleteCatalogJob(String id) {
        jdbc.update("delete from crawl_jobs where id = ?::uuid", id);
    }

    public Map<String, Object> organizeInventory() {
        Map<String, Object> uploadFolder = findUploadedFolder();
        String uploadFolderId = string(uploadFolder.get("id"));
        List<Map<String, Object>> videos = seekVideosInFolder(uploadFolderId);
        List<Map<String, Object>> plan = new ArrayList<>();
        for (Map<String, Object> video : videos) {
            String name = firstNonBlank(string(video.get("name")), string(video.get("filename")), string(video.get("title")), string(video.get("id")));
            ParsedRelease parsed = parseFilename(name);
            String anime = parsed == null ? cleanTitle(name) : parsed.title();
            int season = parsed == null || parsed.season() == null ? 1 : parsed.season();
            int episode = parsed == null || parsed.episode() == null ? 0 : parsed.episode();
            Map<String, Object> parsedMap = new LinkedHashMap<>();
            parsedMap.put("anime", anime);
            parsedMap.put("season", season);
            parsedMap.put("episode", episode == 0 ? null : episode);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("video_id", video.get("id"));
            row.put("video_name", name);
            row.put("parsed", parsedMap);
            row.put("target_anime_folder_name", anime);
            row.put("target_season_folder_name", "Temporada " + season);
            row.put("target_anime_folder_id", null);
            row.put("target_season_folder_id", null);
            row.put("will_create_anime", true);
            row.put("will_create_season", true);
            row.put("match_confidence", parsed == null ? 0 : 1);
            plan.add(row);
        }
        return Map.of(
            "ok", true,
            "upload_folder", Map.of(
                "id", uploadFolderId,
                "name", firstNonBlank(string(uploadFolder.get("name")), "UPLOADED"),
                "total_videos", videos.size()
            ),
            "season_pattern", "Temporada {n}",
            "plan", plan,
            "all_roots", List.of()
        );
    }

    @Transactional
    public Map<String, Object> organize(Map<String, Object> body) {
        List<String> ids = stringList(body.get("videoIds"));
        boolean dryRun = bool(body.get("dryRun"), false);
        if (ids.isEmpty()) {
            Map<String, Object> uploadFolder = findUploadedFolder();
            ids = seekVideosInFolder(string(uploadFolder.get("id"))).stream()
                .map(item -> string(item.get("id")))
                .filter(Objects::nonNull)
                .toList();
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (String id : ids) {
            results.add(Map.of(
                "video_id", id,
                "action", dryRun ? "would_prepare_folder_plan" : "folder_move_not_enabled",
                "ok", true
            ));
        }
        return Map.of(
            "ok", true,
            "dryRun", dryRun,
            "stats", Map.of(
                "processed", ids.size(),
                "moved", 0,
                "foldersCreated", 0,
                "errors", 0,
                "skipped", ids.size()
            ),
            "results", results
        );
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

    private String fetchText(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML, MediaType.ALL));
        headers.set("User-Agent", "nekoflow-worker/0.1 (+catalog)");
        return restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
    }

    private String nyaaPageUrl(String rawUrl, int page) {
        String url = rawUrl == null || rawUrl.isBlank() ? "https://nyaa.si/?f=0&c=0_0&q=" : rawUrl.trim();
        url = url.replace("page=rss&", "").replace("?page=rss", "?").replace("&page=rss", "");
        url = url.replaceAll("([?&])p=\\d+&?", "$1").replaceAll("[?&]$", "");
        String separator = url.contains("?") ? "&" : "?";
        return page <= 1 ? url : url + separator + "p=" + page;
    }

    private int detectNyaaPages(String html) {
        String total = firstMatch(html, "Displaying results\\s+\\d+\\s*-\\s*\\d+\\s+out of\\s+(\\d+)\\s+results");
        if (total != null) {
            return Math.max(1, (int) Math.ceil(intValue(total, 0) / 75.0));
        }
        Matcher matcher = Pattern.compile("[?&]p=(\\d+)").matcher(html == null ? "" : html);
        int max = 1;
        while (matcher.find()) max = Math.max(max, intValue(matcher.group(1), max));
        return max;
    }

    private List<NyaaItem> parseNyaaItems(String html) {
        if (html == null || html.isBlank()) return List.of();
        List<NyaaItem> items = new ArrayList<>();
        Matcher rowMatcher = Pattern.compile("<tr[\\s\\S]*?</tr>", Pattern.CASE_INSENSITIVE).matcher(html);
        while (rowMatcher.find()) {
            String row = rowMatcher.group();
            String magnet = firstMatch(row, "href=\"(magnet:\\?xt=urn:btih:[^\"]+)\"");
            String torrentPath = firstMatch(row, "href=\"(/download/\\d+\\.torrent)\"");
            if (magnet == null && torrentPath == null) continue;
            String infohash = firstMatch(magnet, "(?i)btih:([a-f0-9]{32,40})");
            String title = firstMatch(row, "href=\"/view/\\d+\"\\s+title=\"([^\"]+)\"");
            if (title == null || title.isBlank() || title.equals("Comments")) {
                title = firstMatch(row, "href=\"/view/\\d+\"[^>]*>(.*?)</a>");
            }
            title = stripTags(title == null ? null : htmlDecode(title));
            if (title != null && !title.isBlank()) {
                String torrentUrl = torrentPath == null ? null : "https://nyaa.si" + torrentPath;
                items.add(new NyaaItem(title, infohash, htmlDecode(magnet), torrentUrl, nyaaEpisodeKey(title), parseNyaaSizeBytes(row)));
            }
        }
        return items.isEmpty() ? parseNyaaItemsByAnchors(html) : items;
    }

    private List<NyaaItem> parseNyaaItemsByAnchors(String html) {
        List<NyaaItem> items = new ArrayList<>();
        Matcher viewMatcher = Pattern.compile("href=\"/view/(\\d+)\"\\s+title=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(html == null ? "" : html);
        while (viewMatcher.find()) {
            String torrentId = viewMatcher.group(1);
            String title = stripTags(htmlDecode(viewMatcher.group(2)));
            int from = viewMatcher.end();
            int to = Math.min((html == null ? 0 : html.length()), from + 2500);
            String slice = html == null ? "" : html.substring(from, to);
            String magnet = firstMatch(slice, "href=\"(magnet:\\?xt=urn:btih:[^\"]+)\"");
            String infohash = firstMatch(magnet, "(?i)btih:([a-f0-9]{32,40})");
            String torrentUrl = "https://nyaa.si/download/" + torrentId + ".torrent";
            if (title != null && !title.isBlank()) {
                items.add(new NyaaItem(title, infohash, htmlDecode(magnet), torrentUrl, nyaaEpisodeKey(title), parseNyaaSizeBytes(slice)));
            }
        }
        return items;
    }

    private List<NyaaItem> selectCatalogItems(List<NyaaItem> items, String group, String quality) {
        List<String> tokens = filterTokens(quality);
        List<String> resolutionTokens = tokens.stream().filter(token -> token.matches("\\d{3,4}p|4k|2160p")).toList();
        Map<String, List<NyaaItem>> grouped = new LinkedHashMap<>();
        for (NyaaItem item : items) {
            if (!isAllowedCatalogTorrent(item)) continue;
            if (!matchesGroup(item.title(), group)) continue;
            grouped.computeIfAbsent(item.episodeKey(), ignored -> new ArrayList<>()).add(item);
        }

        List<NyaaItem> selected = new ArrayList<>();
        for (List<NyaaItem> variants : grouped.values()) {
            List<NyaaItem> primary = variants.stream()
                .filter(item -> matchesTokens(item.title(), tokens))
                .toList();
            List<NyaaItem> fallback = variants.stream()
                .filter(item -> matchesTokens(item.title(), resolutionTokens))
                .toList();
            List<NyaaItem> pool = primary.isEmpty() ? fallback : primary;
            pool.stream().max((a, b) -> Integer.compare(catalogScore(a.title()), catalogScore(b.title()))).ifPresent(selected::add);
        }
        return selected;
    }

    private List<String> filterTokens(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Pattern.compile("[,;\\s]+")
            .splitAsStream(value.toLowerCase().trim())
            .filter(token -> !token.isBlank())
            .map(token -> token.equals("hecv") ? "hevc" : token)
            .toList();
    }

    private boolean matchesGroup(String title, String group) {
        if (group == null || group.isBlank()) return true;
        return normalize(title).contains(normalize(group));
    }

    private boolean matchesTokens(String title, List<String> tokens) {
        String normalized = normalize(title);
        for (String token : tokens) {
            String wanted = token.equals("hecv") ? "hevc" : token;
            if (!normalized.contains(normalize(wanted))) return false;
        }
        return true;
    }

    private String nyaaEpisodeKey(String title) {
        String cleaned = title == null ? "" : title.replaceFirst("^\\d+\\s*", "");
        Matcher matcher = Pattern.compile("^\\[[^]]+]\\s*(.+?)\\s+-\\s*(\\d{1,3})(?:\\s|\\[|$)", Pattern.CASE_INSENSITIVE).matcher(cleaned);
        if (matcher.find()) return normalize(matcher.group(1)) + "|" + String.format("%03d", intValue(matcher.group(2), 0));
        return normalize(cleaned.replaceAll("\\[[^]]+]", " ").replaceAll("\\b(1080p|720p|480p|2160p|4k|hevc|avc|aac|eac3|web[- ]?dl|web[- ]?rip|cr|multisub)\\b", " "));
    }

    private boolean isAllowedCatalogTorrent(NyaaItem item) {
        if (item.sizeBytes() > CATALOG_MAX_TORRENT_BYTES) return false;
        String rawTitle = item.title() == null ? "" : item.title().toLowerCase();
        String normalized = normalize(rawTitle);
        if (normalized.contains("batch")) return false;
        return !Pattern.compile("\\b\\d{1,3}\\s*(?:a|to|through|ate|~|-|–|—)\\s*\\d{1,3}\\b", Pattern.CASE_INSENSITIVE)
            .matcher(rawTitle)
            .find();
    }

    private int catalogScore(String title) {
        String normalized = normalize(title);
        int score = 0;
        if (normalized.contains("1080p")) score += 1000;
        if (normalized.contains("720p")) score += 500;
        if (normalized.contains("480p")) score += 100;
        if (normalized.contains("hevc")) score += 100;
        if (normalized.contains("web rip") || normalized.contains("webrip")) score += 20;
        if (normalized.contains("web dl") || normalized.contains("webdl")) score += 10;
        return score;
    }

    private boolean applyFilters(String title, String group, String quality) {
        String normalizedTitle = title == null ? "" : title.toLowerCase();
        if (group != null && !group.isBlank()) {
            String normalizedGroup = group.toLowerCase();
            if (!normalizedTitle.contains(normalizedGroup) && !normalizedTitle.contains("[" + normalizedGroup + "]")) return false;
        }
        if (quality == null || quality.isBlank()) return true;
        for (String option : quality.toLowerCase().split(",")) {
            String[] parts = option.trim().split("\\s+");
            boolean allPartsMatch = parts.length > 0;
            for (String part : parts) {
                if (!part.isBlank() && !normalizedTitle.contains(part)) {
                    allPartsMatch = false;
                    break;
                }
            }
            if (allPartsMatch) return true;
        }
        return false;
    }

    private boolean seenInfohash(String infohash) {
        if (infohash == null || infohash.isBlank()) return false;
        Integer found = jdbc.queryForObject("select count(*) from seen_releases where infohash = ?", Integer.class, infohash);
        return found != null && found > 0;
    }

    private Map<String, Object> createSeekAdvancedUpload(NyaaItem item, String folderId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("url", firstNonBlank(item.magnet(), item.torrentUrl()));
        body.put("name", item.title());
        body.put("folderId", folderId);
        return seekPost("/api/v1/video/advance-upload", body, new ParameterizedTypeReference<>() {});
    }

    private void scheduleCatalogDrain(
        String jobId,
        List<NyaaItem> remainingItems,
        String folderId,
        int itemsFound,
        int itemsSelected,
        int duplicateItems,
        int attempt
    ) {
        List<NyaaItem> snapshot = List.copyOf(remainingItems);
        catalogDrainExecutor.schedule(
            () -> drainCatalogBatch(jobId, snapshot, folderId, itemsFound, itemsSelected, duplicateItems, attempt),
            attempt == 1 ? 1 : CATALOG_DRAIN_DELAY_SECONDS,
            TimeUnit.SECONDS
        );
    }

    private void drainCatalogBatch(
        String jobId,
        List<NyaaItem> remainingItems,
        String folderId,
        int itemsFound,
        int itemsSelected,
        int duplicateItems,
        int attempt
    ) {
        try {
            String status = jdbc.queryForObject("select status from crawl_jobs where id = ?::uuid", String.class, jobId);
            if ("cancelled".equals(status)) return;

            List<NyaaItem> stillPending = remainingItems.stream()
                .filter(item -> item.infohash() == null || !seenInfohash(item.infohash()))
                .toList();
            if (stillPending.isEmpty()) {
                updateCatalogDrainStatus(jobId, itemsFound, itemsSelected, duplicateItems, folderId, attempt, 0, true, null);
                return;
            }

            List<NyaaItem> batch = stillPending.subList(0, Math.min(CATALOG_DRAIN_BATCH_SIZE, stillPending.size()));
            String lastError = null;
            for (NyaaItem item : batch) {
                try {
                    Map<String, Object> task = createSeekAdvancedUpload(item, folderId);
                    jdbc.update("""
                        insert into seen_releases (infohash, guid, release_name, release_queue_id)
                        values (?, ?, ?, null)
                        on conflict (infohash) do nothing
                        """, item.infohash(), string(task.get("id")), item.title());
                } catch (Exception exception) {
                    lastError = truncate(exception.getMessage(), 180);
                    // Mantem sem seen_releases para a proxima tentativa.
                }
                sleepQuietly(200);
            }

            List<NyaaItem> nextPending = remainingItems.stream()
                .filter(item -> item.infohash() == null || !seenInfohash(item.infohash()))
                .toList();
            boolean finished = nextPending.isEmpty();
            boolean exhausted = attempt >= CATALOG_DRAIN_MAX_ATTEMPTS;
            updateCatalogDrainStatus(jobId, itemsFound, itemsSelected, duplicateItems, folderId, attempt, nextPending.size(), finished, lastError);
            if (!finished && !exhausted) {
                scheduleCatalogDrain(jobId, nextPending, folderId, itemsFound, itemsSelected, duplicateItems, attempt + 1);
            } else if (exhausted) {
                jdbc.update("""
                    update crawl_jobs
                    set status = 'failed', status_reason = ?, finished_at = now(), updated_at = now()
                    where id = ?::uuid
                    """,
                    catalogDrainReason(itemsFound, itemsSelected, duplicateItems, itemsSelected - duplicateItems - nextPending.size(), nextPending.size(), folderId, attempt) + " max_attempts",
                    jobId
                );
            }
        } catch (Exception exception) {
            jdbc.update("""
                update crawl_jobs
                set status = 'ingesting', status_reason = coalesce(status_reason, '') || ?, updated_at = now()
                where id = ?::uuid
                """, " drain_error=" + truncate(exception.getMessage(), 180), jobId);
            if (attempt < CATALOG_DRAIN_MAX_ATTEMPTS) {
                scheduleCatalogDrain(jobId, remainingItems, folderId, itemsFound, itemsSelected, duplicateItems, attempt + 1);
            }
        }
    }

    private void updateCatalogDrainStatus(
        String jobId,
        int itemsFound,
        int itemsSelected,
        int duplicateItems,
        String folderId,
        int attempt,
        int remaining,
        boolean finished,
        String lastError
    ) {
        int sent = Math.max(0, itemsSelected - duplicateItems - remaining);
        String reason = catalogDrainReason(itemsFound, itemsSelected, duplicateItems, sent, remaining, folderId, attempt);
        if (lastError != null && !lastError.isBlank()) {
            reason = reason + " last_error=" + lastError.replaceAll("\\s+", "_");
        }
        jdbc.update("""
            update crawl_jobs
            set status = ?, status_reason = ?, items_new = ?, items_failed = ?, finished_at = ?, updated_at = now()
            where id = ?::uuid
            """,
            finished ? "done" : "ingesting",
            reason,
            sent,
            remaining,
            finished ? OffsetDateTime.now() : null,
            jobId
        );
    }

    private String catalogDrainReason(
        int itemsFound,
        int itemsSelected,
        int duplicateItems,
        int sent,
        int remaining,
        String folderId,
        int attempt
    ) {
        return "found=%d selected=%d duplicates=%d sent=%d failed=%d folder=%s attempt=%d".formatted(
            itemsFound,
            itemsSelected,
            duplicateItems,
            sent,
            remaining,
            folderId,
            attempt
        );
    }

    private List<List<NyaaItem>> batches(List<NyaaItem> items, int size) {
        List<List<NyaaItem>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += size) {
            batches.add(items.subList(i, Math.min(i + size, items.size())));
        }
        return batches;
    }

    private Map<String, Object> findUploadedFolder() {
        String configuredId = System.getenv("APP_SEEKSTREAMING_UPLOAD_FOLDER_ID");
        List<Map<String, Object>> folders = seekExchange("/api/v1/video/folder", new ParameterizedTypeReference<>() {});
        if (configuredId != null && !configuredId.isBlank()) {
            for (Map<String, Object> folder : folders) {
                if (configuredId.trim().equals(string(folder.get("id")))) return folder;
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pasta uploaded configurada nao encontrada no Seek.");
        }
        for (Map<String, Object> folder : folders) {
            String normalizedName = normalize(string(folder.get("name")));
            if (List.of("uploaded", "upload", "uploads").contains(normalizedName)) return folder;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pasta UPLOADED nao encontrada no Seek.");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> seekVideosInFolder(String folderId) {
        if (folderId == null || folderId.isBlank()) return List.of();
        Map<String, Object> response = seekExchange("/api/v1/video/folder/" + folderId, new ParameterizedTypeReference<>() {});
        Object data = response.get("data");
        if (!(data instanceof List<?> raw)) return List.of();
        return raw.stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(item -> (Map<String, Object>) item)
            .toList();
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

    private <T> T seekPost(String path, Object body, ParameterizedTypeReference<T> type) {
        String token = resolveSeekToken();
        if (token == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SeekStreaming token ausente.");
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-token", token);
        return Objects.requireNonNull(restTemplate.exchange(resolveSeekEndpoint() + path, HttpMethod.POST, new HttpEntity<>(body, headers), type).getBody());
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

    private String firstMatch(String value, String regex) {
        if (value == null) return null;
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String stripTags(String value) {
        return value == null ? null : value.replaceAll("(?is)<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private String htmlDecode(String value) {
        if (value == null) return null;
        return value
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">");
    }

    private int countMatches(String value, String needle) {
        if (value == null || value.isEmpty() || needle == null || needle.isEmpty()) return 0;
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private long parseNyaaSizeBytes(String html) {
        if (html == null || html.isBlank()) return -1;
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(KiB|MiB|GiB|TiB)", Pattern.CASE_INSENSITIVE).matcher(stripTags(html));
        if (!matcher.find()) return -1;
        double amount;
        try {
            amount = Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException exception) {
            return -1;
        }
        String unit = matcher.group(2).toLowerCase();
        double multiplier = switch (unit) {
            case "kib" -> 1024D;
            case "mib" -> 1024D * 1024D;
            case "gib" -> 1024D * 1024D * 1024D;
            case "tib" -> 1024D * 1024D * 1024D * 1024D;
            default -> 1D;
        };
        return (long) (amount * multiplier);
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

    private record NyaaItem(String title, String infohash, String magnet, String torrentUrl, String episodeKey, long sizeBytes) {
    }
}
