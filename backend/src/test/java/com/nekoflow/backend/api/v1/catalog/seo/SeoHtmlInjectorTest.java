package com.nekoflow.backend.api.v1.catalog.seo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class SeoHtmlInjectorTest {

    private static final String SHELL = """
        <!doctype html><html><head>
        <meta charset="UTF-8" />
        <title>Nekoflow</title>
        <meta name="description" content="descricao antiga" />
        <link rel="canonical" href="https://old" />
        <meta name="robots" content="index,follow" />
        <meta property="og:title" content="old og" />
        <meta property="og:image" content="https://old/image.jpg" />
        <meta name="twitter:card" content="summary" />
        <script type="application/ld+json">{"@type":"OldData"}</script>
        </head><body><div id="root"></div>
        <script type="module" src="/assets/index-abc123.js"></script>
        </body></html>
        """;

    @Test
    void injectsTitleDescriptionCanonicalAndRobots() {
        SeoMeta meta = new SeoMeta("Frieren | Nekoflow", "Sinopse real", "/anime/frieren", false);

        String html = SeoHtmlInjector.inject(SHELL, meta, "https://nekoflow.com.br/");

        assertThat(html).contains("<title>Frieren | Nekoflow</title>");
        assertThat(html).contains("content=\"Sinopse real\"");
        assertThat(html).contains("<link rel=\"canonical\" href=\"https://nekoflow.com.br/anime/frieren\" />");
        assertThat(html).contains("content=\"index,follow\"");
        assertThat(html).contains("<meta property=\"og:title\" content=\"Frieren | Nekoflow\" />");
        assertThat(html).contains("<meta property=\"og:description\" content=\"Sinopse real\" />");
        assertThat(html).contains("<meta property=\"og:type\" content=\"website\" />");
        assertThat(html).contains("<meta property=\"og:url\" content=\"https://nekoflow.com.br/anime/frieren\" />");
        assertThat(html).contains("<meta property=\"og:site_name\" content=\"Nekoflow\" />");
        assertThat(html).contains("<meta property=\"og:image\" content=\"https://nekoflow.com.br/favicon.png\" />");
        assertThat(html).contains("<meta name=\"twitter:card\" content=\"summary_large_image\" />");
        assertThat(html).contains("<meta name=\"twitter:image\" content=\"https://nekoflow.com.br/favicon.png\" />");
        assertThat(html).contains("<script type=\"application/ld+json\">");
        assertThat(html).contains("\"@type\":\"WebPage\"");
        // Nao duplica: o title antigo sumiu.
        assertThat(html).doesNotContain("<title>Nekoflow</title>");
        assertThat(html).doesNotContain("descricao antiga");
        assertThat(html).doesNotContain("old og");
        assertThat(html).doesNotContain("https://old/image.jpg");
        assertThat(html).doesNotContain("OldData");
        // Preserva o asset com hash do Vite.
        assertThat(html).contains("/assets/index-abc123.js");
    }

    @Test
    void noindexRouteGetsNoindexRobots() {
        SeoMeta meta = new SeoMeta("Meu perfil | Nekoflow", "x", "/perfil", true);
        String html = SeoHtmlInjector.inject(SHELL, meta, "https://nekoflow.com.br");
        assertThat(html).contains("content=\"noindex,nofollow\"");
        assertThat(html).doesNotContain("content=\"index,follow\"");
        assertThat(html).doesNotContain("property=\"og:title\"");
        assertThat(html).doesNotContain("name=\"twitter:card\"");
        assertThat(html).doesNotContain("application/ld+json");
    }

    @Test
    void escapesTitleAndDescription() {
        SeoMeta meta = new SeoMeta("<script>alert(1)</script>", "aspas \" e <tag>", "/x", false);
        String html = SeoHtmlInjector.inject(SHELL, meta, "https://nekoflow.com.br");
        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
        assertThat(html).doesNotContain("<script>alert(1)</script>");
        assertThat(html).contains("&quot;");
        assertThat(html).contains("\\u003cscript\\u003ealert(1)\\u003c/script\\u003e")
            .doesNotContain("</script>alert");
    }

    @Test
    void keepsAbsoluteSocialImage() {
        SeoMeta meta = new SeoMeta("Frieren | Nekoflow", "Sinopse", "/anime/frieren", false,
            true, "video.tv_show", "https://img.test/frieren.jpg");
        String html = SeoHtmlInjector.inject(SHELL, meta, "https://nekoflow.com.br");
        assertThat(html).contains("<meta property=\"og:type\" content=\"video.tv_show\" />");
        assertThat(html).contains("<meta property=\"og:image\" content=\"https://img.test/frieren.jpg\" />");
        assertThat(html).contains("<meta name=\"twitter:image\" content=\"https://img.test/frieren.jpg\" />");
    }

    @Test
    void homeGetsOrganizationWebsiteAndSearchActionJsonLd() {
        SeoMeta meta = new SeoMeta(SeoMetadataService.DEFAULT_TITLE, SeoMetadataService.DEFAULT_DESCRIPTION, "/",
            false, true, "website", "/favicon.png", "home", List.of(), List.of(), null, null, null, null, null, null);

        String html = SeoHtmlInjector.inject(SHELL, meta, "https://nekoflow.com.br");

        assertThat(html).contains("\"@type\":\"Organization\"");
        assertThat(html).contains("\"@type\":\"WebSite\"");
        assertThat(html).contains("\"@type\":\"SearchAction\"");
        assertThat(html).contains("\"target\":\"https://nekoflow.com.br/explorar?q={search_term_string}\"");
    }

    @Test
    void animeGetsTvSeriesAndBreadcrumbJsonLd() {
        SeoMeta meta = new SeoMeta("Frieren | Nekoflow", "Sinopse real", "/anime/frieren", false,
            true, "video.tv_show", "https://img.test/frieren.jpg", "anime",
            List.of("Sousou no Frieren"), List.of("Aventura"), 12, null, null, null, null, null);

        String html = SeoHtmlInjector.inject(SHELL, meta, "https://nekoflow.com.br");

        assertThat(html).contains("\"@type\":\"BreadcrumbList\"");
        assertThat(html).contains("\"@type\":\"TVSeries\"");
        assertThat(html).contains("\"alternateName\":[\"Sousou no Frieren\"]");
        assertThat(html).contains("\"genre\":[\"Aventura\"]");
        assertThat(html).contains("\"numberOfEpisodes\":12");
    }

    @Test
    void episodeGetsTvEpisodeVideoObjectAndBreadcrumbJsonLd() {
        SeoMeta meta = new SeoMeta("Frieren - Episodio 1 | Nekoflow", "Resumo real", "/watch/frieren/1", false,
            true, "video.episode", "/favicon.png", "episode", List.of(), List.of("Drama"), null,
            1, "Frieren", "/anime/frieren", "https://player.test/embed/1", 1440);

        String html = SeoHtmlInjector.inject(SHELL, meta, "https://nekoflow.com.br");

        assertThat(html).contains("\"@type\":\"BreadcrumbList\"");
        assertThat(html).contains("\"@type\":\"TVEpisode\"");
        assertThat(html).contains("\"@type\":\"VideoObject\"");
        assertThat(html).contains("\"episodeNumber\":1");
        assertThat(html).contains("\"partOfSeries\":{\"@type\":\"TVSeries\",\"name\":\"Frieren\"");
        assertThat(html).contains("\"embedUrl\":\"https://player.test/embed/1\"");
        assertThat(html).contains("\"duration\":\"PT1440S\"");
        assertThat(html).contains("\"isAccessibleForFree\":true");
    }

    @Test
    void handlesNullShell() {
        assertThat(SeoHtmlInjector.inject(null, new SeoMeta("t", "d", "/", false), "https://x")).isEmpty();
    }
}
