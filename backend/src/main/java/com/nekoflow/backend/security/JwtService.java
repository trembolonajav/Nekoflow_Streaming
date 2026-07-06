package com.nekoflow.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.nekoflow.backend.config.AppProperties;
import com.nekoflow.backend.domain.entity.UserEntity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final AppProperties appProperties;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String generateAccessToken(UserEntity user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(appProperties.jwt().accessTokenExpirationSeconds());

        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("roles", user.getRoles().stream().map(role -> role.getCode().name()).toList())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(Keys.hmacShaKeyFor(normalizedSecret()))
            .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(normalizedSecret()))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private byte[] normalizedSecret() {
        String secret = appProperties.jwt().secret();
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        // HMAC-SHA256 exige chave de pelo menos 256 bits (32 bytes). Em vez de
        // completar um segredo curto com um padding fixo e previsivel, rejeitamos:
        // segredo fraco deve ser corrigido na configuracao, nao mascarado.
        if (bytes.length < 32) {
            throw new IllegalStateException(
                "APP_JWT_SECRET deve ter no minimo 32 caracteres (256 bits) para assinar tokens com HMAC-SHA256."
            );
        }
        return bytes;
    }
}
