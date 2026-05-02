package com.nekoflow.backend.api.v1.catalog;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nekoflow.backend.domain.enums.VisibilityStatus;
import com.nekoflow.backend.domain.repository.AnimeRepository;

@RestController
public class SeoController {

    private final AnimeRepository animeRepository;
    private final String publicBaseUrl;

    public SeoController(
        AnimeRepository animeRepository,
        @Value("${app.public-base-url:https://nekoflow.com.br}") String publicBaseUrl
    ) {
        this.animeRepository = animeRepository;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
            User-agent: *
            Allow: /

            Disallow: /admin
            Disallow: /perfil
            Disallow: /notificacoes

            Sitemap: %s/sitemap.xml
            """.formatted(publicBaseUrl);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        String today = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        appendUrl(xml, "/", "daily", "1.0", today);
        appendUrl(xml, "/explorar", "daily", "0.9", today);
        appendUrl(xml, "/calendario", "daily", "0.8", today);

        animeRepository.findAll().stream()
            .filter(anime -> anime.getVisibility() == VisibilityStatus.PUBLISHED)
            .sorted(java.util.Comparator.comparing(anime -> anime.getTitleDisplay().toLowerCase(java.util.Locale.ROOT)))
            .forEach(anime -> appendUrl(xml, "/anime/" + anime.getSlug(), "weekly", "0.7", today));

        appendUrl(xml, "/termos-de-uso", "monthly", "0.3", today);
        appendUrl(xml, "/politica-de-privacidade", "monthly", "0.3", today);
        xml.append("</urlset>\n");
        return xml.toString();
    }

    private void appendUrl(StringBuilder xml, String path, String changefreq, String priority, String lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(publicBaseUrl + path)).append("</loc>\n");
        xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
}
