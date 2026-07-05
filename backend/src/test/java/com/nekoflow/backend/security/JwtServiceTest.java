package com.nekoflow.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nekoflow.backend.config.AppProperties;
import com.nekoflow.backend.domain.entity.UserEntity;
import com.nekoflow.backend.domain.enums.RoleCode;
import com.nekoflow.backend.testsupport.TestFixtures;

class JwtServiceTest {

    private static final String STRONG_SECRET = "test-secret-key-with-at-least-32-bytes-0123456789";

    private JwtService jwtService(String secret) {
        AppProperties properties = new AppProperties(
            null,
            new AppProperties.Jwt(secret, 3600, 2592000),
            null,
            null,
            null
        );
        return new JwtService(properties);
    }

    @Test
    void rejectsSecretShorterThan32Bytes() {
        JwtService service = jwtService("curto-demais");
        UserEntity user = TestFixtures.user(UUID.randomUUID(), "user@nekoflow.app", RoleCode.USER);

        assertThatThrownBy(() -> service.generateAccessToken(user))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32");
    }

    @Test
    void generatesAndValidatesToken() {
        JwtService service = jwtService(STRONG_SECRET);
        UUID id = UUID.randomUUID();
        UserEntity user = TestFixtures.user(id, "user@nekoflow.app", RoleCode.USER);

        String token = service.generateAccessToken(user);

        assertThat(service.isTokenValid(token)).isTrue();
        assertThat(service.extractUserId(token)).isEqualTo(id);
    }

    @Test
    void rejectsTamperedToken() {
        JwtService service = jwtService(STRONG_SECRET);
        UserEntity user = TestFixtures.user(UUID.randomUUID(), "user@nekoflow.app", RoleCode.USER);
        String token = service.generateAccessToken(user);

        assertThat(service.isTokenValid(token + "tampered")).isFalse();
        assertThat(service.isTokenValid("not-a-jwt")).isFalse();
    }

    @Test
    void tokenSignedWithOtherSecretIsInvalid() {
        JwtService issuer = jwtService(STRONG_SECRET);
        JwtService other = jwtService("another-different-secret-key-with-32-bytes-min-123");
        UserEntity user = TestFixtures.user(UUID.randomUUID(), "user@nekoflow.app", RoleCode.USER);

        String token = issuer.generateAccessToken(user);

        assertThat(other.isTokenValid(token)).isFalse();
    }
}
