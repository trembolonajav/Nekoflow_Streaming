package com.nekoflow.backend.api.v1.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.nekoflow.backend.api.v1.auth.dto.LoginRequest;
import com.nekoflow.backend.api.v1.auth.dto.LogoutRequest;
import com.nekoflow.backend.api.v1.auth.dto.RefreshRequest;
import com.nekoflow.backend.api.v1.auth.dto.RegisterRequest;
import com.nekoflow.backend.api.v1.auth.dto.TokenResponse;
import com.nekoflow.backend.config.AppProperties;
import com.nekoflow.backend.domain.entity.RefreshTokenEntity;
import com.nekoflow.backend.domain.entity.UserEntity;
import com.nekoflow.backend.domain.enums.RoleCode;
import com.nekoflow.backend.domain.repository.RefreshTokenRepository;
import com.nekoflow.backend.domain.repository.RoleRepository;
import com.nekoflow.backend.domain.repository.UserRepository;
import com.nekoflow.backend.domain.repository.UserTermsAcceptanceRepository;
import com.nekoflow.backend.security.AppUserPrincipal;
import com.nekoflow.backend.security.JwtService;
import com.nekoflow.backend.testsupport.TestFixtures;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserTermsAcceptanceRepository userTermsAcceptanceRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    private AuthService service;

    private static final String RAW_REFRESH = "raw-refresh-token-11112222-33334444";

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(
            null,
            new AppProperties.Jwt("test-secret-key-with-at-least-32-bytes-0123456789", 3600, 2592000),
            null,
            null,
            null
        );
        service = new AuthService(
            userRepository, roleRepository, refreshTokenRepository, userTermsAcceptanceRepository,
            passwordEncoder, authenticationManager, jwtService, properties
        );
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn(RAW_REFRESH);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$bcrypt-hash");
        when(roleRepository.findByCode(RoleCode.USER)).thenReturn(Optional.of(TestFixtures.role(RoleCode.USER)));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    // ---------- Cadastro ----------

    @Test
    void registerStoresHashNotRawTokenAndLowercasesEmail() {
        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());

        TokenResponse response = service.register(
            new RegisterRequest("Alice", "USER@Nekoflow.APP", "Senha123", "Senha123", true),
            new MockHttpServletRequest()
        );

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@nekoflow.app");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$10$bcrypt-hash");

        ArgumentCaptor<RefreshTokenEntity> tokenCaptor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        String stored = tokenCaptor.getValue().getToken();
        assertThat(stored).isEqualTo(sha256Hex(RAW_REFRESH));
        assertThat(stored).hasSize(64).isNotEqualTo(RAW_REFRESH);

        // O cliente recebe o valor cru; o banco guarda o hash.
        assertThat(response.refreshToken()).isEqualTo(RAW_REFRESH);
        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void registerRejectsDuplicateLocalEmail() {
        UserEntity existing = TestFixtures.user(UUID.randomUUID(), "user@nekoflow.app", RoleCode.USER);
        existing.setProvider("LOCAL");
        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.register(
            new RegisterRequest("Alice", "user@nekoflow.app", "Senha123", "Senha123", true),
            new MockHttpServletRequest()
        ))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409");
    }

    @Test
    void registerRejectsPasswordMismatch() {
        assertThatThrownBy(() -> service.register(
            new RegisterRequest("Alice", "user@nekoflow.app", "Senha123", "Outra123", true),
            new MockHttpServletRequest()
        ))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    @Test
    void registerRejectsWeakPassword() {
        // 8 chars, so letras, sem numero -> nao passa na regra de forca.
        assertThatThrownBy(() -> service.register(
            new RegisterRequest("Alice", "user@nekoflow.app", "abcdefgh", "abcdefgh", true),
            new MockHttpServletRequest()
        ))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    @Test
    void registerRequiresTermsAcceptance() {
        assertThatThrownBy(() -> service.register(
            new RegisterRequest("Alice", "user@nekoflow.app", "Senha123", "Senha123", false),
            new MockHttpServletRequest()
        ))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    @Test
    void registerAcceptsPasswordWithUnicodeAndEmoji() {
        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        String password = "Senha123ção😀"; // letras + numero + acentos + emoji

        TokenResponse response = service.register(
            new RegisterRequest("Alice", "user@nekoflow.app", password, password, true),
            new MockHttpServletRequest()
        );

        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    // ---------- Login ----------

    @Test
    void loginWithBadCredentialsReturnsGenericUnauthorized() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> service.login(new LoginRequest("user@nekoflow.app", "wrong")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("401")
            .hasMessageContaining("E-mail ou senha inv"); // mensagem generica, nao revela se o e-mail existe
    }

    @Test
    void loginSuccessIssuesTokensAndStoresHash() {
        UUID id = UUID.randomUUID();
        UserEntity user = TestFixtures.user(id, "user@nekoflow.app", RoleCode.USER);
        AppUserPrincipal principal = new AppUserPrincipal(id, "user@nekoflow.app", "$2a$hash", List.of());
        when(authenticationManager.authenticate(any()))
            .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        TokenResponse response = service.login(new LoginRequest("user@nekoflow.app", "Senha123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo(sha256Hex(RAW_REFRESH));
    }

    // ---------- Refresh ----------

    @Test
    void refreshLooksUpByHashRotatesAndReissues() {
        UserEntity user = TestFixtures.user(UUID.randomUUID(), "user@nekoflow.app", RoleCode.USER);
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUser(user);
        entity.setRevoked(false);
        entity.setExpiresAt(OffsetDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByToken(sha256Hex(RAW_REFRESH))).thenReturn(Optional.of(entity));

        TokenResponse response = service.refresh(new RefreshRequest(RAW_REFRESH));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(entity.isRevoked()).as("token antigo deve ser rotacionado/revogado").isTrue();
        verify(refreshTokenRepository).findByToken(sha256Hex(RAW_REFRESH));
        verify(refreshTokenRepository, never()).findByToken(RAW_REFRESH); // nunca busca pelo valor cru
    }

    @Test
    void refreshRejectsRevokedToken() {
        UserEntity user = TestFixtures.user(UUID.randomUUID(), "user@nekoflow.app", RoleCode.USER);
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUser(user);
        entity.setRevoked(true);
        entity.setExpiresAt(OffsetDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByToken(sha256Hex(RAW_REFRESH))).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.refresh(new RefreshRequest(RAW_REFRESH)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refreshRejectsExpiredToken() {
        UserEntity user = TestFixtures.user(UUID.randomUUID(), "user@nekoflow.app", RoleCode.USER);
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUser(user);
        entity.setRevoked(false);
        entity.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByToken(sha256Hex(RAW_REFRESH))).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.refresh(new RefreshRequest(RAW_REFRESH)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refreshRejectsUnknownToken() {
        when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(new RefreshRequest(RAW_REFRESH)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- Logout ----------

    @Test
    void logoutRevokesTokenLookingUpByHash() {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setRevoked(false);
        when(refreshTokenRepository.findByToken(sha256Hex(RAW_REFRESH))).thenReturn(Optional.of(entity));

        service.logout(new LogoutRequest(RAW_REFRESH));

        assertThat(entity.isRevoked()).isTrue();
        verify(refreshTokenRepository).findByToken(sha256Hex(RAW_REFRESH));
        verify(refreshTokenRepository).save(entity);
    }
}
