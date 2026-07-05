package com.nekoflow.backend.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigValidatorTest {

    private static final String STRONG_JWT = "prod-jwt-secret-that-is-quite-long-abcdefghij-abcdefghij-01";
    private static final String STRONG_WORKER = "prod-worker-secret-at-least-32-bytes-long-01";
    private static final String STRONG_DB = "prod-db-password-strong-01";

    private AppProperties props(String corsOrigins) {
        return new AppProperties(
            new AppProperties.Cors(corsOrigins),
            new AppProperties.Jwt(STRONG_JWT, 3600, 2592000),
            new AppProperties.Bootstrap(false, null, null),
            null,
            new AppProperties.Worker(STRONG_WORKER)
        );
    }

    private MockEnvironment prodEnv() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("DB_PASSWORD", STRONG_DB);
        return env;
    }

    @Test
    void wildcardCorsIsRejectedInProduction() {
        ProductionConfigValidator validator = new ProductionConfigValidator(prodEnv(), props("*"));
        assertThatThrownBy(() -> validator.run(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CORS");
    }

    @Test
    void localhostCorsIsRejectedInProduction() {
        ProductionConfigValidator validator = new ProductionConfigValidator(prodEnv(), props("http://localhost:5173"));
        assertThatThrownBy(() -> validator.run(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CORS");
    }

    @Test
    void officialDomainCorsIsAcceptedInProduction() {
        ProductionConfigValidator validator = new ProductionConfigValidator(prodEnv(), props("https://nekoflow.com.br"));
        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void nonProductionSkipsValidation() {
        MockEnvironment devEnv = new MockEnvironment(); // sem profile prod
        ProductionConfigValidator validator = new ProductionConfigValidator(devEnv, props("*"));
        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }
}
