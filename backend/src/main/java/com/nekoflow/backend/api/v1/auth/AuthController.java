package com.nekoflow.backend.api.v1.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nekoflow.backend.api.v1.auth.dto.AuthMeResponse;
import com.nekoflow.backend.api.v1.auth.dto.GoogleAuthRequest;
import com.nekoflow.backend.api.v1.auth.dto.LoginRequest;
import com.nekoflow.backend.api.v1.auth.dto.LogoutRequest;
import com.nekoflow.backend.api.v1.auth.dto.RefreshRequest;
import com.nekoflow.backend.api.v1.auth.dto.RegisterRequest;
import com.nekoflow.backend.api.v1.auth.dto.TokenResponse;
import com.nekoflow.backend.api.v1.common.dto.ApiMessageResponse;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@Validated
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.register(request, httpRequest));
    }

    @PostMapping("/google")
    public ResponseEntity<TokenResponse> google(@Valid @RequestBody GoogleAuthRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.google(request, httpRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiMessageResponse> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(new ApiMessageResponse("Logged out successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me() {
        return ResponseEntity.ok(authService.me());
    }
}
