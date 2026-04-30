package com.nekoflow.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Cors cors,
    Jwt jwt,
    Bootstrap bootstrap,
    Integrations integrations,
    Worker worker
) {

    public record Cors(
        String allowedOrigins
    ) {
    }

    public record Jwt(
        String secret,
        long accessTokenExpirationSeconds,
        long refreshTokenExpirationSeconds
    ) {
    }

    public record Bootstrap(
        User admin,
        User user
    ) {
    }

    public record Integrations(
        AniList anilist,
        SeekStreaming seekstreaming,
        Google google
    ) {
    }

    public record AniList(
        String endpoint
    ) {
    }

    public record SeekStreaming(
        String endpoint,
        String apiToken
    ) {
    }

    public record Google(
        String clientId
    ) {
    }

    public record User(
        String name,
        String email,
        String password
    ) {
    }

    public record Worker(
        String webhookSecret
    ) {
    }
}
