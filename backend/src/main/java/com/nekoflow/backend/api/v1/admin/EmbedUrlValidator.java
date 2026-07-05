package com.nekoflow.backend.api.v1.admin;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Valida URLs de embed/player antes de persistir. As URLs vao para o atributo
 * src de um <iframe> na pagina publica de watch, entao um valor malicioso
 * (javascript:, data:, dominio arbitrario) seria um vetor de XSS/redirect.
 *
 * Regras: aceita apenas https e apenas dominios da allowlist (o player oficial).
 * Blank/null vira null (sem fonte de video), o que e valido.
 */
public final class EmbedUrlValidator {

    private static final List<String> ALLOWED_HOST_SUFFIXES = List.of(
        "seekplayer.me",
        "seekstreaming.com"
    );

    private EmbedUrlValidator() {
    }

    public static String validateOrThrow(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();

        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException exception) {
            throw reject();
        }

        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("https")) {
            // Bloqueia javascript:, data:, http:, etc.
            throw reject();
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw reject();
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        boolean allowed = ALLOWED_HOST_SUFFIXES.stream()
            .anyMatch(suffix -> normalizedHost.equals(suffix) || normalizedHost.endsWith("." + suffix));
        if (!allowed) {
            throw reject();
        }

        return trimmed;
    }

    private static ResponseStatusException reject() {
        return new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "URL de embed nao permitida. Use apenas https e um dominio de player autorizado."
        );
    }
}
