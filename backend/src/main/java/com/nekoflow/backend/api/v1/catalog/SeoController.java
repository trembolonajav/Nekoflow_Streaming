package com.nekoflow.backend.api.v1.catalog;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nekoflow.backend.domain.enums.EpisodeStatus;
import com.nekoflow.backend.domain.enums.VisibilityStatus;
import com.nekoflow.backend.domain.enums.VideoSourceStatus;
import com.nekoflow.backend.domain.repository.AnimeRepository;
import com.nekoflow.backend.domain.repository.AnimeSitemapView;
import com.nekoflow.backend.domain.repository.EpisodeRepository;
import com.nekoflow.backend.domain.repository.EpisodeSitemapView;

@RestController
public class SeoController {

    private static final int SITEMAP_URL_LIMIT = 50_000;

    private final AnimeRepository animeRepository;
    private final EpisodeRepository episodeRepository;
    private final CatalogCache catalogCache;
    private final String publicBaseUrl;

    public SeoController(
        AnimeRepository animeRepository,
        EpisodeRepository episodeRepository,
        CatalogCache catalogCache,
        @Value("${app.public-base-url:https://nekoflow.com.br}") String publicBaseUrl
    ) {
        this.animeRepository = animeRepository;
        this.episodeRepository = episodeRepository;
        this.catalogCache = catalogCache;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
            # Nekoflow robots.txt
            # Public discovery is allowed. Private/account routes are blocked from crawl.
            # AI policy: search/discovery crawlers are allowed; training-specific blocking is not enabled.
            # Reviewed agents: OAI-SearchBot (search), GPTBot (training), ChatGPT-User (user initiated).

            User-agent: *
            Allow: /
            Allow: /explorar
            Allow: /calendario
            Allow: /anime/
            Allow: /watch/
            Allow: /sitemap.xml
            Allow: /sitemap-static.xml
            Allow: /sitemap-animes.xml
            Allow: /sitemap-episodes.xml
            Allow: /sitemap-video.xml

            Disallow: /admin
            Disallow: /perfil
            Disallow: /notificacoes
            Disallow: /entrar

            Sitemap: %s/sitemap.xml
            """.formatted(publicBaseUrl);
    }

    // Markdown com H1 e links (formato recomendado do llms.txt): ajuda LLMs e
    // agentes de IA a entender e citar o site.
    @GetMapping(value = "/llms.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String llms() {
        return """
            # Nekoflow

            Nekoflow e uma plataforma de catalogo e streaming de animes legendados em portugues (PT-BR),
            com calendario de lancamentos da temporada sincronizado com o AniList.

            ## Paginas principais

            - [Inicio](%s/): destaques e ultimos episodios
            - [Explorar](%s/explorar): catalogo completo com busca e filtros
            - [Calendario](%s/calendario): lancamentos da semana com dia e horario

            ## Estrutura de URLs

            - Pagina de anime: `%s/anime/{slug}`
            - Player de episodio: `%s/watch/{slug}/{episode}`

            ## Sitemaps

            - [Indice de sitemaps](%s/sitemap.xml)
            - [Animes](%s/sitemap-animes.xml)
            - [Episodios](%s/sitemap-episodes.xml)
            - [Videos](%s/sitemap-video.xml)

            ## Politicas

            - [Termos de uso](%s/termos-de-uso)
            - [Politica de privacidade](%s/politica-de-privacidade)
            - Nao indexar: rotas privadas `/admin`, `/perfil`, `/notificacoes`, `/entrar`
            """.formatted(
                publicBaseUrl,
                publicBaseUrl,
                publicBaseUrl,
                publicBaseUrl,
                publicBaseUrl,
                publicBaseUrl,
                publicBaseUrl,
                publicBaseUrl,
                publicBaseUrl,
                publicBaseUrl,
                publicBaseUrl
            );
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemapIndex() {
        return catalogCache.sitemap("sitemap:index", this::buildSitemapIndex);
    }

    @GetMapping(value = "/sitemap-static.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemapStatic() {
        return catalogCache.sitemap("sitemap:static", this::buildStaticSitemap);
    }

    @GetMapping(value = "/sitemap-animes.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemapAnimes() {
        return catalogCache.sitemap("sitemap:animes", this::buildAnimeSitemap);
    }

    @GetMapping(value = "/sitemap-episodes.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemapEpisodes() {
        return catalogCache.sitemap("sitemap:episodes", this::buildEpisodeSitemap);
    }

    @GetMapping(value = "/sitemap-video.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemapVideo() {
        return catalogCache.sitemap("sitemap:video", this::buildVideoSitemap);
    }

    private String buildSitemapIndex() {
        String lastmod = now();
        StringBuilder xml = xmlHeader();
        xml.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        appendSitemap(xml, "/sitemap-static.xml", lastmod);
        appendSitemap(xml, "/sitemap-animes.xml", lastmod);
        appendSitemap(xml, "/sitemap-episodes.xml", lastmod);
        appendSitemap(xml, "/sitemap-video.xml", lastmod);
        xml.append("</sitemapindex>\n");
        return xml.toString();
    }

    private String buildStaticSitemap() {
        String lastmod = now();
        StringBuilder xml = urlSet(false);
        appendUrl(xml, "/", lastmod);
        appendUrl(xml, "/explorar", lastmod);
        appendUrl(xml, "/calendario", lastmod);
        appendUrl(xml, "/termos-de-uso", lastmod);
        appendUrl(xml, "/politica-de-privacidade", lastmod);
        xml.append("</urlset>\n");
        return xml.toString();
    }

    private String buildAnimeSitemap() {
        StringBuilder xml = urlSet(false);
        animeRepository.findSitemapPublished(VisibilityStatus.PUBLISHED, PageRequest.of(0, SITEMAP_URL_LIMIT))
            .forEach(view -> appendUrl(xml, "/anime/" + view.getSlug(), formatLastmod(view.getPublishedAt())));
        xml.append("</urlset>\n");
        return xml.toString();
    }

    private String buildEpisodeSitemap() {
        StringBuilder xml = urlSet(false);
        publishedEpisodeViews().forEach(view ->
            appendUrl(xml, episodePath(view), formatLastmod(view.getPublishedAt())));
        xml.append("</urlset>\n");
        return xml.toString();
    }

    private String buildVideoSitemap() {
        StringBuilder xml = urlSet(true);
        publishedEpisodeViews().stream()
            .filter(this::hasVideoSitemapMinimum)
            .forEach(view -> appendVideoUrl(xml, view));
        xml.append("</urlset>\n");
        return xml.toString();
    }

    private List<EpisodeSitemapView> publishedEpisodeViews() {
        return episodeRepository.findSitemapPublishedEpisodes(
            EpisodeStatus.PUBLISHED,
            VisibilityStatus.PUBLISHED,
            VideoSourceStatus.ACTIVE,
            PageRequest.of(0, SITEMAP_URL_LIMIT)
        );
    }

    private boolean hasVideoSitemapMinimum(EpisodeSitemapView view) {
        return notBlank(videoTitle(view))
            && notBlank(videoDescription(view))
            && notBlank(videoThumbnail(view))
            && publicUrl(videoPlayerUrl(view));
    }

    private void appendVideoUrl(StringBuilder xml, EpisodeSitemapView view) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(publicBaseUrl + episodePath(view))).append("</loc>\n");
        appendLastmod(xml, view.getPublishedAt());
        xml.append("    <video:video>\n");
        xml.append("      <video:thumbnail_loc>").append(escapeXml(videoThumbnail(view))).append("</video:thumbnail_loc>\n");
        xml.append("      <video:title>").append(escapeXml(videoTitle(view))).append("</video:title>\n");
        xml.append("      <video:description>").append(escapeXml(videoDescription(view))).append("</video:description>\n");
        xml.append("      <video:player_loc>").append(escapeXml(videoPlayerUrl(view))).append("</video:player_loc>\n");
        if (view.getPublishedAt() != null) {
            xml.append("      <video:publication_date>")
                .append(formatLastmod(view.getPublishedAt()))
                .append("</video:publication_date>\n");
        }
        if (view.getDurationSeconds() != null && view.getDurationSeconds() > 0) {
            xml.append("      <video:duration>").append(view.getDurationSeconds()).append("</video:duration>\n");
        }
        xml.append("    </video:video>\n");
        xml.append("  </url>\n");
    }

    private String videoTitle(EpisodeSitemapView view) {
        return view.getAnimeTitle() + " - Episodio " + view.getEpisodeNumber();
    }

    private String videoDescription(EpisodeSitemapView view) {
        return firstNonBlank(view.getSummary(), view.getAnimeSynopsis());
    }

    private String videoThumbnail(EpisodeSitemapView view) {
        return firstNonBlank(view.getThumbnailUrl(), view.getBannerUrl(), view.getCoverUrl());
    }

    private String videoPlayerUrl(EpisodeSitemapView view) {
        return firstNonBlank(view.getEmbedUrl(), view.getPlayerUrl());
    }

    private String episodePath(EpisodeSitemapView view) {
        return "/watch/" + view.getAnimeSlug() + "/" + view.getEpisodeNumber();
    }

    private StringBuilder xmlHeader() {
        return new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    }

    private StringBuilder urlSet(boolean video) {
        StringBuilder xml = xmlHeader();
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"");
        if (video) {
            xml.append(" xmlns:video=\"http://www.google.com/schemas/sitemap-video/1.1\"");
        }
        xml.append(">\n");
        return xml;
    }

    private void appendSitemap(StringBuilder xml, String path, String lastmod) {
        xml.append("  <sitemap>\n");
        xml.append("    <loc>").append(escapeXml(publicBaseUrl + path)).append("</loc>\n");
        xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        xml.append("  </sitemap>\n");
    }

    private void appendUrl(StringBuilder xml, String path, String lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(publicBaseUrl + path)).append("</loc>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }
        xml.append("  </url>\n");
    }

    private void appendLastmod(StringBuilder xml, OffsetDateTime date) {
        if (date != null) {
            xml.append("    <lastmod>").append(formatLastmod(date)).append("</lastmod>\n");
        }
    }

    private String now() {
        return formatLastmod(OffsetDateTime.now());
    }

    private String formatLastmod(OffsetDateTime value) {
        return value == null ? null : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (notBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private boolean publicUrl(String value) {
        return value != null && (value.startsWith("https://") || value.startsWith("http://"));
    }

    private String escapeXml(String value) {
        return value == null ? "" : value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
}
