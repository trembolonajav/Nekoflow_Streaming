package com.nekoflow.backend.api.v1.auth;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nekoflow.backend.api.v1.auth.dto.AuthMeResponse;
import com.nekoflow.backend.api.v1.auth.dto.LoginRequest;
import com.nekoflow.backend.api.v1.auth.dto.LogoutRequest;
import com.nekoflow.backend.api.v1.auth.dto.RefreshRequest;
import com.nekoflow.backend.api.v1.auth.dto.RegisterRequest;
import com.nekoflow.backend.api.v1.auth.dto.TokenResponse;
import com.nekoflow.backend.domain.entity.RefreshTokenEntity;
import com.nekoflow.backend.domain.entity.RoleEntity;
import com.nekoflow.backend.domain.entity.UserEntity;
import com.nekoflow.backend.domain.enums.RoleCode;
import com.nekoflow.backend.domain.repository.RefreshTokenRepository;
import com.nekoflow.backend.domain.repository.RoleRepository;
import com.nekoflow.backend.domain.repository.UserRepository;
import com.nekoflow.backend.security.AppUserPrincipal;
import com.nekoflow.backend.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final com.nekoflow.backend.config.AppProperties appProperties;

    public AuthService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        com.nekoflow.backend.config.AppProperties appProperties
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        UserEntity user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("E-mail already registered.");
        }

        RoleEntity userRole = roleRepository.findByCode(RoleCode.USER)
            .orElseThrow(() -> new IllegalStateException("USER role not found"));

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setName(request.name());
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setProvider("email");
        user.setActive(true);
        user.setRoles(java.util.Set.of(userRole));
        userRepository.save(user);

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
            user.getRoles().stream().map(role -> role.getCode().name()).sorted().toList()
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
            user.getRoles().stream().map(role -> role.getCode().name()).sorted().toList()
        );
    }
}
