package com.nekoflow.backend.api.v1.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.nekoflow.backend.api.v1.admin.dto.AdminAniListSearchResponse;
import com.nekoflow.backend.config.AppProperties;

class AniListIntegrationServiceTest {

    private static final String ENDPOINT = "https://graphql.anilist.co";

    private MockRestServiceServer server;
    private AniListIntegrationService service;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        AppProperties properties = new AppProperties(
            new AppProperties.Cors("http://localhost:5173"),
            new AppProperties.Jwt("secret", 3600, 2592000),
            new AppProperties.Bootstrap(
                true,
                new AppProperties.User("Admin", "admin@nekoflow.app", "12345678"),
                new AppProperties.User("User", "user@nekoflow.app", "12345678")
            ),
            new AppProperties.Integrations(
                new AppProperties.AniList(ENDPOINT),
                new AppProperties.SeekStreaming("https://seekstreaming.com", null),
                new AppProperties.Google(null)
            ),
            new AppProperties.Worker(null)
        );
        service = new AniListIntegrationService(restTemplate, properties);
    }

    @Test
    void shouldMapAniListResults() {
        server.expect(requestTo(ENDPOINT))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess("""
                {
                  "data": {
                    "Page": {
                      "media": [
                        {
                          "id": 52991,
                          "title": {
                            "romaji": "Sousou no Frieren",
                            "english": "Frieren: Beyond Journey's End",
                            "native": "葬送のフリーレン"
                          },
                          "format": "TV",
                          "status": "RELEASING",
                          "episodes": 28,
                          "seasonYear": 2023,
                          "season": "FALL",
                          "coverImage": {
                            "extraLarge": "https://cdn.example/cover.jpg",
                            "large": "https://cdn.example/cover-small.jpg"
                          },
                          "bannerImage": "https://cdn.example/banner.jpg",
                          "averageScore": 92,
                          "description": "<p>Uma jornada.<br>Com memória.</p>",
                          "genres": ["Adventure", "Drama"],
                          "studios": {
                            "nodes": [
                              { "name": "Madhouse", "isAnimationStudio": true },
                              { "name": "TOHO", "isAnimationStudio": false }
                            ]
                          }
                        }
                      ]
                    }
                  }
                }
                """, MediaType.APPLICATION_JSON));

        List<AdminAniListSearchResponse> results = service.search("frieren");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(52991L);
        assertThat(results.get(0).titleRomaji()).isEqualTo("Sousou no Frieren");
        assertThat(results.get(0).titleEnglish()).isEqualTo("Frieren: Beyond Journey's End");
        assertThat(results.get(0).coverImage()).isEqualTo("https://cdn.example/cover.jpg");
        assertThat(results.get(0).description()).isEqualTo("Uma jornada.\nCom memória.");
        assertThat(results.get(0).studios()).containsExactly("Madhouse");

        server.verify();
    }

    @Test
    void shouldShortCircuitSmallQueries() {
        assertThat(service.search("f")).isEmpty();
    }
}
