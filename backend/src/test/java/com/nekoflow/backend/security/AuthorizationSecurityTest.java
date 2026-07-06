package com.nekoflow.backend.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.nekoflow.backend.config.AppProperties;
import com.nekoflow.backend.config.SecurityConfig;
import com.nekoflow.backend.domain.entity.UserEntity;
import com.nekoflow.backend.domain.enums.RoleCode;
import com.nekoflow.backend.domain.repository.UserRepository;
import com.nekoflow.backend.health.HealthController;
import com.nekoflow.backend.testsupport.TestFixtures;

/**
 * Testa as REGRAS de autorizacao do SecurityConfig com a cadeia de seguranca real
 * (filtro JWT ativo). Usa HealthController como alvo publico; as rotas /admin/**
 * nao tem controller neste slice, entao passar pela seguranca resulta em 404
 * (e nao 401/403) — o que prova que a autorizacao permitiu o acesso.
 */
@WebMvcTest(controllers = HealthController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class, AppUserDetailsService.class})
@EnableConfigurationProperties(AppProperties.class)
@TestPropertySource(properties = {
    "app.jwt.secret=integration-test-secret-key-with-32-bytes-min-0123456789",
    "app.jwt.access-token-expiration-seconds=3600",
    "app.jwt.refresh-token-expiration-seconds=2592000",
    "app.cors.allowed-origins=http://localhost:5173"
})
class AuthorizationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    private String tokenFor(RoleCode role) {
        UUID id = UUID.randomUUID();
        UserEntity user = TestFixtures.user(id, "user@nekoflow.app", role);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return jwtService.generateAccessToken(user);
    }

    @Test
    void publicHealthEndpointIsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk());
    }

    @Test
    void adminEndpointWithoutTokenReturns401() throws Exception {
        // Sem token -> nao autenticado -> 401.
        mockMvc.perform(get("/api/v1/admin/worker/dashboard"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/worker/dashboard")
                .header("Authorization", "Bearer not-a-valid-jwt"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void commonUserCannotAccessAdmin() throws Exception {
        // Autenticado mas sem role -> 403.
        mockMvc.perform(get("/api/v1/admin/worker/dashboard")
                .header("Authorization", "Bearer " + tokenFor(RoleCode.USER)))
            .andExpect(status().isForbidden());
    }

    @Test
    void commonUserCannotAccessWorkerModerationOrApproval() throws Exception {
        String userToken = tokenFor(RoleCode.USER);
        // worker, moderacao de reports e aprovacao de sugestao — todos negados (403).
        mockMvc.perform(get("/api/v1/admin/worker/queue").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/reports").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/episodes").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminRolePassesAuthorization() throws Exception {
        // Um ADMIN passa pela autorizacao (diferente do USER, que leva 403).
        // Como nao ha controller mapeado neste slice, o status final e um artefato
        // do teste (404/500) — o que importa e NAO ser 401/403 (barrado). Isso
        // prova que a role ADMIN foi autorizada a acessar /admin/**.
        mockMvc.perform(get("/api/v1/admin/worker/dashboard")
                .header("Authorization", "Bearer " + tokenFor(RoleCode.ADMIN)))
            .andExpect(result -> {
                int statusCode = result.getResponse().getStatus();
                assertTrue(statusCode != 401 && statusCode != 403,
                    "ADMIN deveria passar a autorizacao, mas foi barrado com status " + statusCode);
            });
    }

    @Test
    void meEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer garbage.token.value"))
            .andExpect(status().isUnauthorized());
    }
}
