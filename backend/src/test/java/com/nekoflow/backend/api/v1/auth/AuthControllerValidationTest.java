package com.nekoflow.backend.api.v1.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.nekoflow.backend.api.v1.auth.dto.TokenResponse;
import com.nekoflow.backend.security.JwtAuthenticationFilter;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    private void postJson(String path, String body, int expectedStatus) throws Exception {
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is(expectedStatus));
    }

    // ---------- Register ----------

    @Test
    void registerRejectsEmptyBody() throws Exception {
        postJson("/api/v1/auth/register", "", 400);
    }

    @Test
    void registerRejectsMalformedJson() throws Exception {
        postJson("/api/v1/auth/register", "{ not valid json", 400);
    }

    @Test
    void registerRejectsInvalidEmail() throws Exception {
        postJson("/api/v1/auth/register",
            "{\"name\":\"Alice\",\"email\":\"not-an-email\",\"password\":\"Senha123\",\"confirmPassword\":\"Senha123\",\"acceptTerms\":true}",
            400);
    }

    @Test
    void registerRejectsShortPassword() throws Exception {
        postJson("/api/v1/auth/register",
            "{\"name\":\"Alice\",\"email\":\"user@nekoflow.app\",\"password\":\"abc\",\"confirmPassword\":\"abc\",\"acceptTerms\":true}",
            400);
    }

    @Test
    void registerRejectsNullPassword() throws Exception {
        // Regressao do fix: password null agora e barrado por @NotBlank (400), nao NPE (500).
        postJson("/api/v1/auth/register",
            "{\"name\":\"Alice\",\"email\":\"user@nekoflow.app\",\"password\":null,\"confirmPassword\":\"Senha123\",\"acceptTerms\":true}",
            400);
    }

    @Test
    void registerRejectsMissingName() throws Exception {
        postJson("/api/v1/auth/register",
            "{\"email\":\"user@nekoflow.app\",\"password\":\"Senha123\",\"confirmPassword\":\"Senha123\",\"acceptTerms\":true}",
            400);
    }

    @Test
    void registerAcceptsValidPayload() throws Exception {
        when(authService.register(any(), any())).thenReturn(sampleToken());
        postJson("/api/v1/auth/register",
            "{\"name\":\"Alice\",\"email\":\"user@nekoflow.app\",\"password\":\"Senha123\",\"confirmPassword\":\"Senha123\",\"acceptTerms\":true}",
            200);
    }

    // ---------- Login ----------

    @Test
    void loginRejectsInvalidEmail() throws Exception {
        postJson("/api/v1/auth/login", "{\"email\":\"bad\",\"password\":\"Senha123\"}", 400);
    }

    @Test
    void loginRejectsMissingPassword() throws Exception {
        postJson("/api/v1/auth/login", "{\"email\":\"user@nekoflow.app\"}", 400);
    }

    @Test
    void loginAcceptsValidPayload() throws Exception {
        when(authService.login(any())).thenReturn(sampleToken());
        postJson("/api/v1/auth/login", "{\"email\":\"user@nekoflow.app\",\"password\":\"Senha123\"}", 200);
    }

    private TokenResponse sampleToken() {
        return new TokenResponse(
            "access-token", "refresh-token", "Bearer", 3600,
            "id-1", "Alice", "user@nekoflow.app", List.of("USER"), "LOCAL"
        );
    }
}
