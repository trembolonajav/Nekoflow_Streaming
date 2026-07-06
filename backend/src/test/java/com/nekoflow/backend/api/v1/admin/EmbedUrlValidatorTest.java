package com.nekoflow.backend.api.v1.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class EmbedUrlValidatorTest {

    @Test
    void allowsHttpsOnAllowedDomain() {
        assertThat(EmbedUrlValidator.validateOrThrow("https://nekoflow.seekplayer.me/#abc123"))
            .isEqualTo("https://nekoflow.seekplayer.me/#abc123");
        assertThat(EmbedUrlValidator.validateOrThrow("https://seekstreaming.com/embed/1"))
            .isEqualTo("https://seekstreaming.com/embed/1");
    }

    @Test
    void blankOrNullBecomesNull() {
        assertThat(EmbedUrlValidator.validateOrThrow(null)).isNull();
        assertThat(EmbedUrlValidator.validateOrThrow("")).isNull();
        assertThat(EmbedUrlValidator.validateOrThrow("   ")).isNull();
    }

    @Test
    void blocksJavascriptScheme() {
        assertThatThrownBy(() -> EmbedUrlValidator.validateOrThrow("javascript:alert(1)"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void blocksDataScheme() {
        assertThatThrownBy(() -> EmbedUrlValidator.validateOrThrow("data:text/html,<script>alert(1)</script>"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void blocksPlainHttp() {
        assertThatThrownBy(() -> EmbedUrlValidator.validateOrThrow("http://nekoflow.seekplayer.me/x"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void blocksDisallowedDomain() {
        assertThatThrownBy(() -> EmbedUrlValidator.validateOrThrow("https://evil.example.com/embed"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void blocksSuffixSpoofingDomain() {
        // "seekplayer.me.evil.com" nao deve ser aceito como seekplayer.me
        assertThatThrownBy(() -> EmbedUrlValidator.validateOrThrow("https://seekplayer.me.evil.com/x"))
            .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> EmbedUrlValidator.validateOrThrow("https://xseekplayer.me/x"))
            .isInstanceOf(ResponseStatusException.class);
    }
}
