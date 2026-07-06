package com.nekoflow.backend.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate limiting simples e em memoria para endpoints sensiveis a abuso.
 *
 * Sem dependencia nova nem infra externa — adequado a uma VM unica. Janela fixa
 * por (IP + regra). Ao estourar, responde 429 com Retry-After. Uso normal (humano)
 * nunca atinge os limites; so dispara sob volume anormal de um mesmo IP.
 *
 * Regras cobrem: autenticacao (brute force), criacao de comentario/resposta e
 * sugestao (flood/spam) e busca (scraping/abuso). Demais rotas passam livres.
 */
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private record Rule(String key, String method, Pattern path, int maxPerMinute) {
        boolean matches(String requestMethod, String uri) {
            return method.equalsIgnoreCase(requestMethod) && path.matcher(uri).matches();
        }
    }

    private static final List<Rule> RULES = List.of(
        new Rule("auth-login", "POST", Pattern.compile("/api/v1/auth/login"), 10),
        new Rule("auth-register", "POST", Pattern.compile("/api/v1/auth/register"), 10),
        new Rule("auth-refresh", "POST", Pattern.compile("/api/v1/auth/refresh"), 10),
        new Rule("comment", "POST", Pattern.compile("/api/v1/episodes/[^/]+/comments"), 15),
        new Rule("reply", "POST", Pattern.compile("/api/v1/comments/[^/]+/replies"), 15),
        new Rule("suggestion", "POST", Pattern.compile("/api/v1/suggestions"), 15),
        new Rule("search", "GET", Pattern.compile("/api/v1/animes"), 60)
    );

    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_TRACKED_KEYS = 50_000;

    private final Map<String, Window> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        Rule rule = matchRule(request.getMethod(), request.getRequestURI());
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey = clientIp(request) + "|" + rule.key();
        if (!allow(bucketKey, rule.maxPerMinute())) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Muitas requisicoes. Aguarde um instante e tente novamente.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Rule matchRule(String method, String uri) {
        for (Rule rule : RULES) {
            if (rule.matches(method, uri)) {
                return rule;
            }
        }
        return null;
    }

    private boolean allow(String key, int maxPerMinute) {
        long now = System.currentTimeMillis();
        if (buckets.size() > MAX_TRACKED_KEYS) {
            buckets.entrySet().removeIf(entry -> now - entry.getValue().start >= WINDOW_MS);
        }
        Window window = buckets.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing.start >= WINDOW_MS) {
                return new Window(now);
            }
            existing.count++;
            return existing;
        });
        return window.count <= maxPerMinute;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        private final long start;
        private int count;

        private Window(long start) {
            this.start = start;
            this.count = 1;
        }
    }
}
