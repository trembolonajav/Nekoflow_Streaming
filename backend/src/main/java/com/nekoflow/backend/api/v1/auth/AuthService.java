package com.nekoflow.backend.api.v1.auth;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.nekoflow.backend.api.v1.auth.dto.AuthMeResponse;
import com.nekoflow.backend.api.v1.auth.dto.GoogleAuthRequest;
import com.nekoflow.backend.api.v1.auth.dto.LoginRequest;
import com.nekoflow.backend.api.v1.auth.dto.LogoutRequest;
import com.nekoflow.backend.api.v1.auth.dto.RefreshRequest;
import com.nekoflow.backend.api.v1.auth.dto.RegisterRequest;
import com.nekoflow.backend.api.v1.auth.dto.TokenResponse;
import com.nekoflow.backend.domain.entity.RefreshTokenEntity;
import com.nekoflow.backend.domain.entity.RoleEntity;
import com.nekoflow.backend.domain.entity.UserEntity;
import com.nekoflow.backend.domain.entity.UserTermsAcceptanceEntity;
import com.nekoflow.backend.domain.enums.AuthProvider;
import com.nekoflow.backend.domain.enums.RoleCode;
import com.nekoflow.backend.domain.repository.RefreshTokenRepository;
import com.nekoflow.backend.domain.repository.RoleRepository;
import com.nekoflow.backend.domain.repository.UserRepository;
import com.nekoflow.backend.domain.repository.UserTermsAcceptanceRepository;
import com.nekoflow.backend.security.AppUserPrincipal;
import com.nekoflow.backend.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {

    private static final String TERMS_VERSION = "1.0";
    private static final String PRIVACY_POLICY_VERSION = "1.0";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserTermsAcceptanceRepository userTermsAcceptanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final com.nekoflow.backend.config.AppProperties appProperties;
    private final RestClient restClient;

    public AuthService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        RefreshTokenRepository refreshTokenRepository,
        UserTermsAcceptanceRepository userTermsAcceptanceRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        com.nekoflow.backend.config.AppProperties appProperties
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userTermsAcceptanceRepository = userTermsAcceptanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
        this.restClient = RestClient.create();
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        }
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        UserEntity user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        validateRegisterRequest(request);

        UserEntity existing = userRepository.findByEmailIgnoreCase(request.email()).orElse(null);
        if (existing != null && AuthProvider.valueOf(existing.getProvider()).includesLocal()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
        }

        RoleEntity userRole = roleRepository.findByCode(RoleCode.USER)
            .orElseThrow(() -> new IllegalStateException("USER role not found"));

        UserEntity user = existing == null ? new UserEntity() : existing;
        if (existing == null) {
            user.setId(UUID.randomUUID());
            user.setEmail(request.email().toLowerCase());
            user.setActive(true);
            user.setRoles(java.util.Set.of(userRole));
            user.setProvider(AuthProvider.LOCAL.name());
        } else {
            user.setProvider(AuthProvider.LOCAL_GOOGLE.name());
        }

        user.setName(request.name().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        recordTermsAcceptance(user, httpRequest, AuthProvider.LOCAL);

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse google(GoogleAuthRequest request, HttpServletRequest httpRequest) {
        GoogleTokenInfo tokenInfo = verifyGoogleIdToken(request.idToken());
        String email = normalizeEmail(tokenInfo.email());
        UserEntity user = userRepository.findByGoogleSub(tokenInfo.sub())
            .or(() -> userRepository.findByEmailIgnoreCase(email))
            .orElse(null);

        boolean needsTermsAcceptance = user == null || !hasCurrentTermsAcceptance(user);
        if (needsTermsAcceptance && !request.acceptTerms()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Para o primeiro acesso com Google, aceite os Termos de Uso e a Política de Privacidade."
            );
        }

        RoleEntity userRole = roleRepository.findByCode(RoleCode.USER)
            .orElseThrow(() -> new IllegalStateException("USER role not found"));

        if (user == null) {
            user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setName(defaultIfBlank(tokenInfo.name(), email));
            user.setActive(true);
            user.setRoles(java.util.Set.of(userRole));
            user.setProvider(AuthProvider.GOOGLE.name());
        } else {
            AuthProvider provider = AuthProvider.valueOf(user.getProvider());
            if (provider == AuthProvider.LOCAL) {
                user.setProvider(AuthProvider.LOCAL_GOOGLE.name());
            }
            if (defaultIfBlank(user.getName(), "").isBlank()) {
                user.setName(defaultIfBlank(tokenInfo.name(), email));
            }
        }

        user.setGoogleSub(tokenInfo.sub());
        user.setAvatarUrl(tokenInfo.picture());
        userRepository.save(user);

        if (needsTermsAcceptance) {
            recordTermsAcceptance(user, httpRequest, AuthProvider.GOOGLE);
        }

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshTokenEntity refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new IllegalArgumentException("Refresh token not found."));

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expired or revoked.");
        }

        refreshToken.setRevoked(true);
        UserEntity user = refreshToken.getUser();
        return issueTokens(user);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken())
            .ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
    }

    @Transactional(readOnly = true)
    public AuthMeResponse me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new IllegalStateException("User not authenticated.");
        }

        UserEntity user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        return new AuthMeResponse(
            user.getId().toString(),
            user.getName(),
            user.getEmail(),
            user.getRoles().stream().map(role -> role.getCode().name()).sorted().toList(),
            user.getProvider()
        );
    }

    private TokenResponse issueTokens(UserEntity user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken();

        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiresAt(OffsetDateTime.now().plusSeconds(appProperties.jwt().refreshTokenExpirationSeconds()));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(
            accessToken,
            refreshTokenValue,
            "Bearer",
            appProperties.jwt().accessTokenExpirationSeconds(),
            user.getId().toString(),
            user.getName(),
            user.getEmail(),
            user.getRoles().stream().map(role -> role.getCode().name()).sorted().toList(),
            user.getProvider()
        );
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (!request.acceptTerms()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Para continuar, você precisa aceitar os Termos de Uso e a Política de Privacidade.");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A confirmação de senha não confere.");
        }
        if (!isPasswordStrongEnough(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter no mínimo 8 caracteres, com pelo menos uma letra e um número.");
        }
    }

    private boolean isPasswordStrongEnough(String password) {
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return password.length() >= 8 && hasLetter && hasDigit;
    }

    private boolean hasCurrentTermsAcceptance(UserEntity user) {
        return userTermsAcceptanceRepository.findTopByUserIdOrderByAcceptedAtDesc(user.getId())
            .map(record -> TERMS_VERSION.equals(record.getTermsVersion()) && PRIVACY_POLICY_VERSION.equals(record.getPrivacyPolicyVersion()))
            .orElse(false);
    }

    private void recordTermsAcceptance(UserEntity user, HttpServletRequest request, AuthProvider provider) {
        UserTermsAcceptanceEntity acceptance = new UserTermsAcceptanceEntity();
        acceptance.setId(UUID.randomUUID());
        acceptance.setUser(user);
        acceptance.setTermsVersion(TERMS_VERSION);
        acceptance.setPrivacyPolicyVersion(PRIVACY_POLICY_VERSION);
        acceptance.setAcceptedAt(OffsetDateTime.now());
        acceptance.setIpAddress(extractClientIp(request));
        acceptance.setUserAgent(truncate(request.getHeader("User-Agent"), 1000));
        acceptance.setAuthProvider(provider.name());
        userTermsAcceptanceRepository.save(acceptance);
    }

    private GoogleTokenInfo verifyGoogleIdToken(String idToken) {
        String clientId = appProperties.integrations() != null && appProperties.integrations().google() != null
            ? appProperties.integrations().google().clientId()
            : null;
        if (clientId == null || clientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Login com Google não está configurado no servidor.");
        }

        try {
            GoogleTokenInfo tokenInfo = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host("oauth2.googleapis.com")
                    .path("/tokeninfo")
                    .queryParam("id_token", idToken)
                    .build())
                .retrieve()
                .body(GoogleTokenInfo.class);

            if (tokenInfo == null || tokenInfo.sub() == null || tokenInfo.email() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não foi possível validar a conta Google.");
            }
            if (!clientId.equals(tokenInfo.aud())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A conta Google informada não pertence a este aplicativo.");
            }
            if (!"true".equalsIgnoreCase(tokenInfo.emailVerified())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A conta Google precisa ter e-mail verificado.");
            }
            return tokenInfo;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não foi possível validar a conta Google.");
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return truncate(request.getRemoteAddr(), 64);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record GoogleTokenInfo(
        String aud,
        String sub,
        String email,
        String name,
        String picture,
        @com.fasterxml.jackson.annotation.JsonProperty("email_verified") String emailVerified
    ) {
    }
}
