package com.nekoflow.backend.api.v1.catalog.seo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Injeta o <head> calculado no shell da SPA (index.html do build do Vite),
 * preservando os assets com hash. Remove title/description/canonical/robots
 * existentes e insere os novos antes de </head>. Puro/testavel.
 */
public final class SeoHtmlInjector {

    private static final ObjectMapper JSON = new ObjectMapper();

    private SeoHtmlInjector() {
    }

    public static String inject(String shell, SeoMeta meta, String baseUrl) {
        if (shell == null) {
            return "";
        }
        String html = shell
            .replaceAll("(?is)<title>.*?</title>", "")
            .replaceAll("(?is)<meta\\s+name=[\"']description[\"'][^>]*>", "")
            .replaceAll("(?is)<link\\s+rel=[\"']canonical[\"'][^>]*>", "")
            .replaceAll("(?is)<meta\\s+name=[\"']robots[\"'][^>]*>", "")
            .replaceAll("(?is)<meta\\s+property=[\"']og:[^\"']+[\"'][^>]*>", "")
            .replaceAll("(?is)<meta\\s+name=[\"']twitter:[^\"']+[\"'][^>]*>", "")
            .replaceAll("(?is)<script\\s+type=[\"']application/ld\\+json[\"'][^>]*>.*?</script>", "");

        String canonical = baseUrl.replaceAll("/+$", "") + meta.canonicalPath();
        String block = "<title>" + escapeText(meta.title()) + "</title>"
            + "<meta name=\"description\" content=\"" + escapeAttr(meta.description()) + "\" />"
            + "<link rel=\"canonical\" href=\"" + escapeAttr(canonical) + "\" />"
            + "<meta name=\"robots\" content=\"" + (meta.noindex() ? "noindex,nofollow" : "index,follow") + "\" />";
        if (meta.socialEnabled()) {
            String image = absoluteUrl(baseUrl, meta.imageUrl());
            block += "<meta property=\"og:title\" content=\"" + escapeAttr(meta.title()) + "\" />"
                + "<meta property=\"og:description\" content=\"" + escapeAttr(meta.description()) + "\" />"
                + "<meta property=\"og:type\" content=\"" + escapeAttr(meta.ogType()) + "\" />"
                + "<meta property=\"og:url\" content=\"" + escapeAttr(canonical) + "\" />"
                + "<meta property=\"og:site_name\" content=\"" + escapeAttr(SeoMetadataService.SITE_NAME) + "\" />"
                + "<meta property=\"og:image\" content=\"" + escapeAttr(image) + "\" />"
                + "<meta name=\"twitter:card\" content=\"summary_large_image\" />"
                + "<meta name=\"twitter:title\" content=\"" + escapeAttr(meta.title()) + "\" />"
                + "<meta name=\"twitter:description\" content=\"" + escapeAttr(meta.description()) + "\" />"
                + "<meta name=\"twitter:image\" content=\"" + escapeAttr(image) + "\" />";
        }
        block += jsonLdScripts(meta, baseUrl, canonical);

        if (html.matches("(?is).*</head>.*")) {
            return html.replaceFirst("(?i)</head>", Matcher.quoteReplacement(block) + "</head>");
        }
        return block + html;
    }

    private static String escapeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeAttr(String value) {
        if (value == null) {
            return "";
        }
        return escapeText(value).replace("\"", "&quot;");
    }

    private static String absoluteUrl(String baseUrl, String value) {
        String fallbackBase = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        if (value == null || value.isBlank()) {
            return fallbackBase + SeoMetadataService.DEFAULT_IMAGE_PATH;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return fallbackBase + (value.startsWith("/") ? value : "/" + value);
    }

    private static String jsonLdScripts(SeoMeta meta, String baseUrl, String canonical) {
        if (meta.noindex() || !meta.socialEnabled()) {
            return "";
        }

        String base = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        List<Map<String, Object>> blocks = new ArrayList<>();
        String kind = meta.structuredDataKind();

        if ("home".equals(kind)) {
            blocks.add(organization(base, meta));
            blocks.add(webSite(base));
        } else if ("anime".equals(kind)) {
            blocks.add(breadcrumb(base, List.of(
                crumb("Inicio", base + "/"),
                crumb("Explorar", base + "/explorar"),
                crumb(stripSite(meta.title()), canonical)
            )));
            blocks.add(tvSeries(meta, canonical, base));
        } else if ("episode".equals(kind)) {
            String seriesUrl = absoluteUrl(base, meta.seriesPath());
            blocks.add(breadcrumb(base, List.of(
                crumb("Inicio", base + "/"),
                crumb("Explorar", base + "/explorar"),
                crumb(meta.seriesName(), seriesUrl),
                crumb(stripSite(meta.title()), canonical)
            )));
            blocks.add(tvEpisode(meta, canonical, seriesUrl));
            blocks.add(videoObject(meta, canonical));
        } else if ("generic".equals(kind)) {
            blocks.add(webPage(meta, canonical));
        }

        StringBuilder out = new StringBuilder();
        for (Map<String, Object> block : blocks) {
            if (!block.isEmpty()) {
                out.append("<script type=\"application/ld+json\">")
                    .append(toJson(block))
                    .append("</script>");
            }
        }
        return out.toString();
    }

    private static Map<String, Object> organization(String base, SeoMeta meta) {
        Map<String, Object> data = object();
        data.put("@context", "https://schema.org");
        data.put("@type", "Organization");
        data.put("name", SeoMetadataService.SITE_NAME);
        data.put("url", base + "/");
        data.put("logo", absoluteUrl(base, meta.imageUrl()));
        return data;
    }

    private static Map<String, Object> webSite(String base) {
        Map<String, Object> action = object();
        action.put("@type", "SearchAction");
        action.put("target", base + "/explorar?q={search_term_string}");
        action.put("query-input", "required name=search_term_string");

        Map<String, Object> data = object();
        data.put("@context", "https://schema.org");
        data.put("@type", "WebSite");
        data.put("name", SeoMetadataService.SITE_NAME);
        data.put("url", base + "/");
        data.put("potentialAction", action);
        return data;
    }

    private static Map<String, Object> webPage(SeoMeta meta, String canonical) {
        Map<String, Object> data = object();
        data.put("@context", "https://schema.org");
        data.put("@type", "WebPage");
        data.put("name", stripSite(meta.title()));
        data.put("description", meta.description());
        data.put("url", canonical);
        data.put("image", absoluteUrl(canonicalRoot(canonical), meta.imageUrl()));
        return data;
    }

    private static Map<String, Object> tvSeries(SeoMeta meta, String canonical, String base) {
        Map<String, Object> data = object();
        data.put("@context", "https://schema.org");
        data.put("@type", "TVSeries");
        data.put("name", stripSite(meta.title()));
        putIfNotEmpty(data, "alternateName", meta.alternateNames());
        data.put("description", meta.description());
        data.put("image", absoluteUrl(base, meta.imageUrl()));
        data.put("url", canonical);
        putIfNotEmpty(data, "genre", meta.genres());
        putIfNotNull(data, "numberOfEpisodes", meta.numberOfEpisodes());
        return data;
    }

    private static Map<String, Object> tvEpisode(SeoMeta meta, String canonical, String seriesUrl) {
        Map<String, Object> series = object();
        series.put("@type", "TVSeries");
        series.put("name", meta.seriesName());
        series.put("url", seriesUrl);

        Map<String, Object> data = object();
        data.put("@context", "https://schema.org");
        data.put("@type", "TVEpisode");
        data.put("name", stripSite(meta.title()));
        data.put("description", meta.description());
        putIfNotNull(data, "episodeNumber", meta.episodeNumber());
        data.put("partOfSeries", series);
        data.put("url", canonical);
        data.put("image", absoluteUrl(canonicalRoot(canonical), meta.imageUrl()));
        data.put("isAccessibleForFree", true);
        putIfNotEmpty(data, "genre", meta.genres());
        return data;
    }

    private static Map<String, Object> videoObject(SeoMeta meta, String canonical) {
        Map<String, Object> data = object();
        data.put("@context", "https://schema.org");
        data.put("@type", "VideoObject");
        data.put("name", stripSite(meta.title()));
        data.put("description", meta.description());
        data.put("thumbnailUrl", List.of(absoluteUrl(canonicalRoot(canonical), meta.imageUrl())));
        data.put("url", canonical);
        data.put("isAccessibleForFree", true);
        putIfNotBlank(data, "embedUrl", meta.embedUrl());
        putIfNotBlank(data, "duration", isoDuration(meta.durationSeconds()));
        return data;
    }

    private static Map<String, Object> breadcrumb(String base, List<Map<String, Object>> crumbs) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < crumbs.size(); i++) {
            Map<String, Object> crumb = crumbs.get(i);
            if (crumb.get("name") != null && crumb.get("item") != null) {
                Map<String, Object> item = object();
                item.put("@type", "ListItem");
                item.put("position", i + 1);
                item.put("name", crumb.get("name"));
                item.put("item", crumb.get("item"));
                items.add(item);
            }
        }

        Map<String, Object> data = object();
        data.put("@context", "https://schema.org");
        data.put("@type", "BreadcrumbList");
        data.put("itemListElement", items);
        return data;
    }

    private static Map<String, Object> crumb(String name, String item) {
        Map<String, Object> crumb = object();
        crumb.put("name", name);
        crumb.put("item", item);
        return crumb;
    }

    private static Map<String, Object> object() {
        return new LinkedHashMap<>();
    }

    private static void putIfNotEmpty(Map<String, Object> data, String key, List<String> value) {
        if (value != null && !value.isEmpty()) {
            data.put(key, value);
        }
    }

    private static void putIfNotNull(Map<String, Object> data, String key, Object value) {
        if (value != null) {
            data.put(key, value);
        }
    }

    private static void putIfNotBlank(Map<String, Object> data, String key, String value) {
        if (value != null && !value.isBlank()) {
            data.put(key, value);
        }
    }

    private static String toJson(Map<String, Object> data) {
        try {
            return JSON.writeValueAsString(data)
                .replace("&", "\\u0026")
                .replace("<", "\\u003c")
                .replace(">", "\\u003e");
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Falha ao serializar JSON-LD.", exception);
        }
    }

    private static String stripSite(String title) {
        if (title == null) {
            return "";
        }
        return title.replace(" | " + SeoMetadataService.SITE_NAME, "");
    }

    private static String canonicalRoot(String canonical) {
        if (canonical == null) {
            return "";
        }
        int scheme = canonical.indexOf("://");
        if (scheme < 0) {
            return "";
        }
        int pathStart = canonical.indexOf('/', scheme + 3);
        return pathStart < 0 ? canonical : canonical.substring(0, pathStart);
    }

    private static String isoDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return null;
        }
        return "PT" + seconds + "S";
    }
}
