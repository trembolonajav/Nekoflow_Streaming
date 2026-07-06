package com.nekoflow.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiRateLimitFilterTest {

    private int call(ApiRateLimitFilter filter, String method, String uri, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    private int callUntilBlocked(ApiRateLimitFilter filter, String method, String uri, String ip, int limit) throws Exception {
        for (int i = 0; i < limit; i++) {
            assertThat(call(filter, method, uri, ip)).as("dentro do limite (%d)", i).isEqualTo(200);
        }
        return call(filter, method, uri, ip);
    }

    @Test
    void authLoginBlocksAfterTen() throws Exception {
        assertThat(callUntilBlocked(new ApiRateLimitFilter(), "POST", "/api/v1/auth/login", "203.0.113.1", 10))
            .isEqualTo(429);
    }

    @Test
    void registerAndRefreshAreLimited() throws Exception {
        assertThat(callUntilBlocked(new ApiRateLimitFilter(), "POST", "/api/v1/auth/register", "203.0.113.2", 10)).isEqualTo(429);
        assertThat(callUntilBlocked(new ApiRateLimitFilter(), "POST", "/api/v1/auth/refresh", "203.0.113.3", 10)).isEqualTo(429);
    }

    @Test
    void commentCreationIsLimitedAtFifteen() throws Exception {
        // Path com id dinamico deve casar com a regra de comentario.
        assertThat(callUntilBlocked(new ApiRateLimitFilter(), "POST", "/api/v1/episodes/abc-123/comments", "203.0.113.4", 15))
            .isEqualTo(429);
    }

    @Test
    void replyCreationIsLimitedAtFifteen() throws Exception {
        assertThat(callUntilBlocked(new ApiRateLimitFilter(), "POST", "/api/v1/comments/xyz-999/replies", "203.0.113.5", 15))
            .isEqualTo(429);
    }

    @Test
    void suggestionCreationIsLimitedAtFifteen() throws Exception {
        assertThat(callUntilBlocked(new ApiRateLimitFilter(), "POST", "/api/v1/suggestions", "203.0.113.6", 15))
            .isEqualTo(429);
    }

    @Test
    void searchIsLimitedButMorePermissive() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter();
        // 15 buscas ainda passam (limite de busca e maior que o de escrita).
        for (int i = 0; i < 15; i++) {
            assertThat(call(filter, "GET", "/api/v1/animes", "203.0.113.7")).isEqualTo(200);
        }
        assertThat(callUntilBlocked(new ApiRateLimitFilter(), "GET", "/api/v1/animes", "203.0.113.8", 60)).isEqualTo(429);
    }

    @Test
    void unlimitedPathsPassFreely() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter();
        for (int i = 0; i < 40; i++) {
            assertThat(call(filter, "GET", "/api/v1/home", "203.0.113.9")).isEqualTo(200);
            assertThat(call(filter, "GET", "/api/v1/animes/frieren", "203.0.113.9")).isEqualTo(200);
            assertThat(call(filter, "POST", "/api/v1/auth/logout", "203.0.113.9")).isEqualTo(200);
        }
    }

    @Test
    void differentIpsTrackedSeparately() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter();
        for (int i = 0; i < 10; i++) {
            call(filter, "POST", "/api/v1/auth/login", "198.51.100.1");
        }
        assertThat(call(filter, "POST", "/api/v1/auth/login", "198.51.100.2")).isEqualTo(200);
    }

    @Test
    void blocked429CarriesRetryAfterAndMessage() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter();
        for (int i = 0; i < 10; i++) {
            call(filter, "POST", "/api/v1/auth/login", "198.51.100.9");
        }
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("198.51.100.9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentAsString()).contains("Muitas requisicoes");
    }
}
