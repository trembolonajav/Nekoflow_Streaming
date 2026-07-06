package com.nekoflow.backend.api.v1.catalog.seo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import com.nekoflow.backend.api.v1.catalog.CatalogCache;

class SeoRenderControllerTest {

    private static final String SHELL = """
        <!doctype html><html><head>
        <title>old</title>
        <meta property="og:title" content="old" />
        </head><body><div id="root"></div></body></html>
        """;

    @Test
    void renderInjectsSocialMetadataFromOriginalUri() {
        SeoMetadataService metadata = org.mockito.Mockito.mock(SeoMetadataService.class);
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        when(metadata.metadataFor("/anime/frieren?utm=x"))
            .thenReturn(new SeoMeta("Frieren | Nekoflow", "Sinopse real", "/anime/frieren",
                false, true, "video.tv_show", "https://img.test/frieren.jpg", "anime",
                List.of("Sousou no Frieren"), List.of("Aventura"), 1, null, null, null, null, null));
        when(restTemplate.getForObject("http://frontend/__spa_index.html", String.class)).thenReturn(SHELL);

        SeoRenderController controller = new SeoRenderController(
            metadata,
            new CatalogCache(),
            restTemplate,
            "http://frontend",
            "https://nekoflow.com.br"
        );

        String html = controller.render("/anime/frieren?utm=x").getBody();

        assertThat(html).contains("<meta property=\"og:title\" content=\"Frieren | Nekoflow\" />");
        assertThat(html).contains("<meta property=\"og:type\" content=\"video.tv_show\" />");
        assertThat(html).contains("<meta property=\"og:url\" content=\"https://nekoflow.com.br/anime/frieren\" />");
        assertThat(html).contains("<meta name=\"twitter:image\" content=\"https://img.test/frieren.jpg\" />");
        assertThat(html).contains("\"@type\":\"TVSeries\"");
        assertThat(html).contains("\"@type\":\"BreadcrumbList\"");
        assertThat(html).doesNotContain("content=\"old\"");
    }
}
