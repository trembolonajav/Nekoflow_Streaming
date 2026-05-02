package com.nekoflow.backend.config;

import java.util.Arrays;
import java.util.Locale;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionConfigValidator implements ApplicationRunner {

    private static final String DEV_JWT_SECRET = "nekoflow-dev-secret-please-change-this-key";

    private final Environment environment;
    private final AppProperties appProperties;

    public ProductionConfigValidator(Environment environment, AppProperties appProperties) {
        this.environment = environment;
        this.appProperties = appProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProductionProfile()) {
            return;
        }

        requireStrongSecret("APP_JWT_SECRET", appProperties.jwt().secret(), 48);
        if (DEV_JWT_SECRET.equals(appProperties.jwt().secret())) {
            throw new IllegalStateException("APP_JWT_SECRET cannot use the development default in prod.");
        }

        requireStrongSecret("APP_WORKER_WEBHOOK_SECRET", appProperties.worker().webhookSecret(), 32);
        requireStrongSecret("DB_PASSWORD", environment.getProperty("DB_PASSWORD"), 16);
        requireProductionCors();
        requireBootstrapSafe();
    }

    private boolean isProductionProfile() {
        return Arrays.stream(environment.getActiveProfiles())
            .map(profile -> profile.toLowerCase(Locale.ROOT))
            .anyMatch(profile -> profile.equals("prod") || profile.equals("production"));
    }

    private void requireStrongSecret(String name, String value, int minLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required in prod.");
        }
        if (looksLikePlaceholder(value)) {
            throw new IllegalStateException(name + " must be replaced with a real value in prod.");
        }
        if (value.length() < minLength) {
            throw new IllegalStateException(name + " must have at least " + minLength + " characters in prod.");
        }
    }

    private boolean looksLikePlaceholder(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("troque_")
            || normalized.contains("change-this")
            || normalized.contains("coloque_")
            || normalized.contains("placeholder");
    }

    private void requireProductionCors() {
        String origins = appProperties.cors().allowedOrigins();
        if (origins == null || origins.isBlank()) {
            throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS is required in prod.");
        }
        if (origins.contains("*") || origins.contains("localhost") || origins.contains("127.0.0.1")) {
            throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS must not allow wildcard or localhost in prod.");
        }
    }

    private void requireBootstrapSafe() {
        AppProperties.Bootstrap bootstrap = appProperties.bootstrap();
        if (bootstrap == null || !bootstrap.enabled()) {
            return;
        }

        requireStrongSecret("APP_BOOTSTRAP_ADMIN_PASSWORD", bootstrap.admin().password(), 16);
        if ("12345678".equals(bootstrap.admin().password()) || "12345678".equals(bootstrap.user().password())) {
            throw new IllegalStateException("Bootstrap users cannot use the development password in prod.");
        }
    }
}
