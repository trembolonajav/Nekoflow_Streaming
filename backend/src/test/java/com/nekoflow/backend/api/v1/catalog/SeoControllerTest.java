package com.nekoflow.backend.api.v1.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import com.nekoflow.backend.domain.entity.AnimeEntity;
import com.nekoflow.backend.domain.enums.VisibilityStatus;
import com.nekoflow.backend.domain.repository.AnimeRepository;
import com.nekoflow.backend.security.JwtAuthenticationFilter;

@WebMvcTest(
    controllers = SeoController.class,
    properties = "app.public-base-url=https://nekoflow.com.br",
    excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class SeoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnimeRepository animeRepository;

    @Test
    void shouldReturnRobotsTxtWithSitemapLocation() throws Exception {
        mockMvc.perform(get("/robots.txt"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("User-agent: *")))
            .andExpect(content().string(containsString("Disallow: /admin")))
            .andExpect(content().string(containsString("Sitemap: https://nekoflow.com.br/sitemap.xml")));
    }

    @Test
    void shouldReturnSitemapWithOnlyPublishedAnimeRoutes() throws Exception {
        AnimeEntity published = anime("frieren", "Frieren", VisibilityStatus.PUBLISHED);
        AnimeEntity draft = anime("rascunho", "Rascunho", VisibilityStatus.DRAFT);
        when(animeRepository.findAll()).thenReturn(List.of(draft, published));

        mockMvc.perform(get("/sitemap.xml"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<loc>https://nekoflow.com.br/</loc>")))
            .andExpect(content().string(containsString("<loc>https://nekoflow.com.br/anime/frieren</loc>")))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/anime/rascunho"))));
    }

    private AnimeEntity anime(String slug, String title, VisibilityStatus visibility) {
        AnimeEntity anime = new AnimeEntity();
        anime.setId(UUID.randomUUID());
        anime.setSlug(slug);
        anime.setTitleDisplay(title);
        anime.setVisibility(visibility);
        return anime;
    }
}
