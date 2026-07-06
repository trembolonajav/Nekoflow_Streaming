package com.nekoflow.backend.api.v1.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekoflow.backend.config.AppProperties;

/**
 * Validacao controlada do poll RSS (B7): feed e Seek stubados (MockRestServiceServer),
 * banco mockado (JdbcTemplate). Nenhuma rede/scraping real. Cobre item valido,
 * duplicado, filtro, feed com erro, 0 fontes e o payload de advance-upload.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminWorkerPollTest {

    @Mock private JdbcTemplate jdbc;
    @Mock private com.nekoflow.backend.api.v1.worker.WorkerReleaseWebhookService webhookService;

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private AdminWorkerService service;

    private static final String FOLDERS_JSON = "[{\"id\":\"folder-1\",\"name\":\"UPLOADED\"}]";
    private static final String FEED_ONE_ITEM = """
        <rss xmlns:nyaa="https://nyaa.si/xmlns/nyaa"><channel>
        <item>
          <title>[Test-raws] Alpha - 01 [1080p]</title>
          <guid>guid-1</guid>
          <nyaa:infoHash>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</nyaa:infoHash>
        </item>
        </channel></rss>
        """;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        AppProperties props = new AppProperties(
            null, null, null,
            new AppProperties.Integrations(null, new AppProperties.SeekStreaming("https://seek.test", "test-token"), null),
            null
        );
        service = new AdminWorkerService(jdbc, restTemplate, new ObjectMapper(), props, webhookService);
    }

    private void singleSource(String group, String quality) {
        when(jdbc.queryForList("select * from rss_sources where enabled = true"))
            .thenReturn(List.of(source(group, quality)));
    }

    private Map<String, Object> source(String group, String quality) {
        java.util.Map<String, Object> src = new java.util.HashMap<>();
        src.put("id", "11111111-1111-1111-1111-111111111111");
        src.put("name", "Test source");
        src.put("url", "https://feed.test/rss");
        src.put("release_group_filter", group);
        src.put("quality_filter", quality);
        return src;
    }

    private void expectFolderLookup() {
        server.expect(requestTo("https://seek.test/api/v1/video/folder"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(FOLDERS_JSON, MediaType.APPLICATION_JSON));
    }

    private void expectFeed(String xml) {
        server.expect(requestTo("https://feed.test/rss"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(xml, MediaType.TEXT_PLAIN));
    }

    @Test
    void validItemIsIngestedWithCorrectAdvanceUploadPayload() {
        singleSource(null, null);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(0); // nao visto
        expectFolderLookup();
        expectFeed(FEED_ONE_ITEM);
        server.expect(requestTo("https://seek.test/api/v1/video/advance-upload"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.folderId").value("folder-1"))
            .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("magnet:?xt=urn:btih:aaaa")))
            .andExpect(jsonPath("$.name").value("[Test-raws] Alpha - 01 [1080p]"))
            .andRespond(withSuccess("{\"id\":\"task-1\"}", MediaType.APPLICATION_JSON));

        Map<String, Object> result = service.pollSources(Map.of());

        server.verify();
        assertThat(result.get("sources")).isEqualTo(1);
        assertThat(result.get("new")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = ((List<Map<String, Object>>) result.get("summary")).get(0);
        assertThat(summary.get("fetched")).isEqualTo(1);
        assertThat(summary.get("new")).isEqualTo(1);
        assertThat(summary.get("duplicate")).isEqualTo(0);
        assertThat(summary.get("failed")).isEqualTo(0);
        verify(jdbc).update(contains("insert into seen_releases"), any(), any(), any(), any());
        verify(jdbc).update(contains("update rss_sources"), any(), any(), any());
    }

    @Test
    void duplicateItemIsNotReIngested() {
        singleSource(null, null);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(1); // ja visto
        expectFolderLookup();
        expectFeed(FEED_ONE_ITEM);
        // Nenhum advance-upload esperado.

        Map<String, Object> result = service.pollSources(Map.of());

        server.verify();
        assertThat(result.get("new")).isEqualTo(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = ((List<Map<String, Object>>) result.get("summary")).get(0);
        assertThat(summary.get("duplicate")).isEqualTo(1);
        verify(jdbc, never()).update(contains("insert into seen_releases"), any(), any(), any(), any());
    }

    @Test
    void itemFilteredByGroupIsSkipped() {
        singleSource("erai-raws", null); // titulo do feed nao contem erai-raws
        expectFolderLookup();
        expectFeed(FEED_ONE_ITEM);
        // Nenhum advance-upload; filtrado antes.

        Map<String, Object> result = service.pollSources(Map.of());

        server.verify();
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = ((List<Map<String, Object>>) result.get("summary")).get(0);
        assertThat(summary.get("filtered")).isEqualTo(1);
        assertThat(summary.get("new")).isEqualTo(0);
    }

    @Test
    void feedErrorIsIsolatedAndRecorded() {
        singleSource(null, null);
        expectFolderLookup();
        server.expect(requestTo("https://feed.test/rss"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withServerError());

        Map<String, Object> result = service.pollSources(Map.of());

        server.verify();
        assertThat(result.get("new")).isEqualTo(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = ((List<Map<String, Object>>) result.get("summary")).get(0);
        assertThat(summary.get("error")).asString().contains("feed");
        // Fonte marcada (last_poll_error preenchido).
        verify(jdbc).update(contains("update rss_sources"), any(), any(), any());
    }

    @Test
    void alreadyPublishedReleaseIsNotRepublished() {
        when(jdbc.queryForMap(anyString(), any()))
            .thenReturn(Map.of("id", "22222222-2222-2222-2222-222222222222", "status", "published"));

        Map<String, Object> result = service.publish(List.of("22222222-2222-2222-2222-222222222222"));

        // Idempotencia: nao chama o webhook de publicacao de novo.
        verifyNoInteractions(webhookService);
        assertThat(result.get("published")).isEqualTo(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> first = ((List<Map<String, Object>>) result.get("results")).get(0);
        assertThat(first.get("skipped")).isEqualTo("already_published");
    }

    @Test
    void zeroSourcesDoesNotCallSeek() {
        when(jdbc.queryForList("select * from rss_sources where enabled = true")).thenReturn(List.of());

        Map<String, Object> result = service.pollSources(Map.of());

        server.verify(); // nenhuma requisicao HTTP esperada nem feita
        assertThat(result.get("sources")).isEqualTo(0);
        assertThat(result.get("new")).isEqualTo(0);
    }
}
