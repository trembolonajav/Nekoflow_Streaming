package com.nekoflow.backend.api.v1.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekoflow.backend.config.AppProperties;
import com.nekoflow.backend.domain.repository.AnimeRepository;
import com.nekoflow.backend.domain.repository.EpisodeRepository;
import com.nekoflow.backend.domain.repository.EpisodeVideoSourceRepository;

class WorkerWebhookSignatureTest {

    private static final String SECRET = "webhook-secret-key";
    private static final String BODY_NO_TITLE = "{\"event\":\"release.publish\"}";

    private WorkerReleaseWebhookService service(String secret) {
        AppProperties properties = new AppProperties(
            null, null, null, null,
            new AppProperties.Worker(secret)
        );
        return new WorkerReleaseWebhookService(
            new ObjectMapper(),
            properties,
            mock(RestTemplate.class),
            mock(AnimeRepository.class),
            mock(EpisodeRepository.class),
            mock(EpisodeVideoSourceRepository.class),
            new com.nekoflow.backend.api.v1.catalog.CatalogCache()
        );
    }

    private static int statusOf(Throwable throwable) {
        return ((ResponseStatusException) throwable).getStatusCode().value();
    }

    private static String hmacSha256Hex(String secret, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    @Test
    void blankSecretFailsClosedWith503() {
        assertThatThrownBy(() -> service("").publishRelease(BODY_NO_TITLE, new HttpHeaders()))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(503));
    }

    @Test
    void nullSecretFailsClosedWith503() {
        assertThatThrownBy(() -> service(null).publishRelease(BODY_NO_TITLE, new HttpHeaders()))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(503));
    }

    @Test
    void missingSignatureIsUnauthorized() {
        assertThatThrownBy(() -> service(SECRET).publishRelease(BODY_NO_TITLE, new HttpHeaders()))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(401));
    }

    @Test
    void invalidSignatureIsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Nekoflow-Signature", "sha256=deadbeefdeadbeef");

        assertThatThrownBy(() -> service(SECRET).publishRelease(BODY_NO_TITLE, headers))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(401));
    }

    @Test
    void validSignaturePassesVerificationAndReachesProcessing() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Nekoflow-Signature", "sha256=" + hmacSha256Hex(SECRET, BODY_NO_TITLE));

        // Assinatura valida -> passa a verificacao e cai no processamento, que
        // rejeita o corpo sem titulo com 400. O 400 (e nao 401/503) prova que a
        // assinatura foi aceita.
        assertThatThrownBy(() -> service(SECRET).publishRelease(BODY_NO_TITLE, headers))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(400));
    }
}
