package com.nekoflow.backend.api.v1.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.nekoflow.backend.domain.enums.EpisodeStatus;
import com.nekoflow.backend.domain.enums.VisibilityStatus;
import com.nekoflow.backend.domain.enums.VideoSourceStatus;
import com.nekoflow.backend.domain.repository.AnimeRepository;
import com.nekoflow.backend.domain.repository.AnimeSitemapView;
import com.nekoflow.backend.domain.repository.EpisodeRepository;
import com.nekoflow.backend.domain.repository.EpisodeSitemapView;
import com.nekoflow.backend.security.JwtAuthenticationFilter;

@WebMvcTest(
    controllers = SeoController.class,
    properties = "app.public-base-url=https://nekoflow.com.br",
    excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import(CatalogCache.class)
class SeoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CatalogCache catalogCache;

    @MockBean
    private AnimeRepository animeRepository;

    @MockBean
    private EpisodeRepository episodeRepository;

    @BeforeEach
    void clearSitemapCache() {
        catalogCache.invalidateAll();
    }

    @Test
    void shouldReturnRobotsTxtWithSitemapLocationAndPrivateBlocks() throws Exception {
        mockMvc.perform(get("/robots.txt"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(containsString("User-agent: *")))
            .andExpect(content().string(containsString("Allow: /watch/")))
            .andExpect(content().string(containsString("Allow: /sitemap.xml")))
            .andExpect(content().string(containsString("Disallow: /admin")))
            .andExpect(content().string(containsString("Disallow: /perfil")))
            .andExpect(content().string(containsString("Disallow: /notificacoes")))
            .andExpect(content().string(containsString("Disallow: /entrar")))
            .andExpect(content().string(containsString("Sitemap: https://nekoflow.com.br/sitemap.xml")))
            .andExpect(content().string(not(containsString("Disallow: /watch"))))
            .andExpect(content().string(not(containsString("localhost"))));
    }

    @Test
    void llmsTxtExistsWithSitemapAndDoesNotListPrivateRoutesAsPublic() throws Exception {
        mockMvc.perform(get("/llms.txt"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(containsString("# Nekoflow")))
            .andExpect(content().string(containsString("https://nekoflow.com.br/sitemap.xml")))
            .andExpect(content().string(containsString("https://nekoflow.com.br/anime/{slug}")))
            .andExpect(content().string(containsString("https://nekoflow.com.br/watch/{slug}/{episode}")))
            .andExpect(content().string(containsString("Nao indexar:")))
            .andExpect(content().string(not(containsString("localhost"))))
            .andExpect(content().string(not(containsString("Paginas publicas:\nhttps://nekoflow.com.br/admin"))))
            .andExpect(content().string(not(containsString("Paginas publicas:\nhttps://nekoflow.com.br/perfil"))))
            .andExpect(content().string(not(containsString("Paginas publicas:\nhttps://nekoflow.com.br/notificacoes"))))
            .andExpect(content().string(not(containsString("Paginas publicas:\nhttps://nekoflow.com.br/entrar"))));
    }

    @Test
    void sitemapXmlReturnsSitemapIndex() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
            .andExpect(content().string(containsString("<sitemapindex")))
            .andExpect(content().string(containsString("<loc>https://nekoflow.com.br/sitemap-static.xml</loc>")))
            .andExpect(content().string(containsString("<loc>https://nekoflow.com.br/sitemap-animes.xml</loc>")))
            .andExpect(content().string(containsString("<loc>https://nekoflow.com.br/sitemap-episodes.xml</loc>")))
            .andExpect(content().string(containsString("<loc>https://nekoflow.com.br/sitemap-video.xml</loc>")));
    }

    @Test
    void staticSitemapContainsPublicRoutesOnly() throws Exception {
        mockMvc.perform(get("/sitemap-static.xml"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<loc>https://nekoflow.com.br/</loc>")))
            .andExpect(content().string(containsString("<loc>https://nekoflow.com.br/explorar</loc>")))
            .andExpect(content().string(containsString("<loc>https://nekoflow.com.br/calendario</loc>")))
            .andExpect(content().string(containsString("<loc>https://nekoflow.com.br/termos-de-uso</loc>")))
            .andExpect(content().string(containsString("<loc>https://nekoflow.com.br/politica-de-privacidade</loc>")))
            .andExpect(content().string(not(containsString("/entrar"))))
            .andExpect(content().string(not(containsString("/perfil"))))
            .andExpect(content().string(not(containsString("/admin"))));
    }

    @Test
    void animeSitemapContainsPublishedAnimesWithRealLastmodAndEscapedXml() throws Exception {
        when(animeRepository.findSitemapPublished(eq(VisibilityStatus.PUBLISHED), any(Pageable.class)))
            .thenReturn(List.of(anime("frieren-&-friends", OffsetDateTime.parse("2026-01-15T10:00:00Z"))));

        mockMvc.perform(get("/sitemap-animes.xml"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<loc>https://nekoflow.com.br/anime/frieren-&amp;-friends</loc>")))
            .andExpect(content().string(containsString("<lastmod>2026-01-15T10:00:00Z</lastmod>")))
            .andExpect(content().string(not(containsString("/anime/rascunho"))));
    }

    @Test
    void episodeSitemapContainsPublishedEpisodesWithPlayers() throws Exception {
        whenEpisodes(episode("frieren", 1, OffsetDateTime.parse("2026-01-16T10:00:00Z")));

        mockMvc.perform(get("/sitemap-episodes.xml"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<loc>https://nekoflow.com.br/watch/frieren/1</loc>")))
            .andExpect(content().string(containsString("<lastmod>2026-01-16T10:00:00Z</lastmod>")))
            .andExpect(content().string(not(containsString("/admin"))))
            .andExpect(content().string(not(containsString("/perfil"))));
    }

    @Test
    void videoSitemapContainsOnlyRealVideoFieldsAndOmitsInventedFields() throws Exception {
        whenEpisodes(episode("frieren", 1, OffsetDateTime.parse("2026-01-16T10:00:00Z")));

        mockMvc.perform(get("/sitemap-video.xml"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("xmlns:video=\"http://www.google.com/schemas/sitemap-video/1.1\"")))
            .andExpect(content().string(containsString("<loc>https://nekoflow.com.br/watch/frieren/1</loc>")))
            .andExpect(content().string(containsString("<video:title>Frieren &amp; Fern - Episodio 1</video:title>")))
            .andExpect(content().string(containsString("<video:description>Resumo real &amp; seguro</video:description>")))
            .andExpect(content().string(containsString("<video:thumbnail_loc>https://cdn.nekoflow.com.br/thumb.jpg</video:thumbnail_loc>")))
            .andExpect(content().string(containsString("<video:player_loc>https://player.nekoflow.com.br/embed/1</video:player_loc>")))
            .andExpect(content().string(containsString("<video:publication_date>2026-01-16T10:00:00Z</video:publication_date>")))
            .andExpect(content().string(containsString("<video:duration>1440</video:duration>")))
            .andExpect(content().string(not(containsString("video:rating"))))
            .andExpect(content().string(not(containsString("video:view_count"))));
    }

    @Test
    void videoSitemapSkipsEpisodeWithoutMinimumVideoData() throws Exception {
        whenEpisodes(episodeWithoutThumbnail("frieren", 2));

        mockMvc.perform(get("/sitemap-video.xml"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("/watch/frieren/2"))));
    }

    private void whenEpisodes(EpisodeSitemapView... views) {
        when(episodeRepository.findSitemapPublishedEpisodes(
            eq(EpisodeStatus.PUBLISHED),
            eq(VisibilityStatus.PUBLISHED),
            eq(VideoSourceStatus.ACTIVE),
            any(Pageable.class)
        )).thenReturn(List.of(views));
    }

    private AnimeSitemapView anime(String slug, OffsetDateTime publishedAt) {
        return new AnimeSitemapView() {
            @Override
            public String getSlug() {
                return slug;
            }

            @Override
            public OffsetDateTime getPublishedAt() {
                return publishedAt;
            }
        };
    }

    private EpisodeSitemapView episode(String slug, Integer number, OffsetDateTime publishedAt) {
        return episode(slug, number, publishedAt, "https://cdn.nekoflow.com.br/thumb.jpg");
    }

    private EpisodeSitemapView episodeWithoutThumbnail(String slug, Integer number) {
        return episode(slug, number, OffsetDateTime.parse("2026-01-17T10:00:00Z"), null);
    }

    private EpisodeSitemapView episode(String slug, Integer number, OffsetDateTime publishedAt, String thumbnail) {
        return new EpisodeSitemapView() {
            @Override public String getAnimeSlug() { return slug; }
            @Override public String getAnimeTitle() { return "Frieren & Fern"; }
            @Override public String getAnimeSynopsis() { return "Sinopse real"; }
            @Override public String getCoverUrl() { return null; }
            @Override public String getBannerUrl() { return null; }
            @Override public Integer getEpisodeNumber() { return number; }
            @Override public String getEpisodeTitle() { return "Episodio " + number; }
            @Override public String getSummary() { return "Resumo real & seguro"; }
            @Override public String getThumbnailUrl() { return thumbnail; }
            @Override public Integer getDurationSeconds() { return 1440; }
            @Override public OffsetDateTime getPublishedAt() { return publishedAt; }
            @Override public String getEmbedUrl() { return "https://player.nekoflow.com.br/embed/" + number; }
            @Override public String getPlayerUrl() { return null; }
        };
    }
}
