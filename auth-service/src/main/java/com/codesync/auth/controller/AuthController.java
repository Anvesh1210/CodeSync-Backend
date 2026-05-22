package com.codesync.auth.controller;

import com.codesync.auth.dto.request.ChangePasswordRequest;
import com.codesync.auth.dto.request.ForgotPasswordRequest;
import com.codesync.auth.dto.request.LoginRequest;
import com.codesync.auth.dto.request.OAuthLoginRequest;
import com.codesync.auth.dto.request.RefreshTokenRequest;
import com.codesync.auth.dto.request.RegisterRequest;
import com.codesync.auth.dto.request.ResendOtpRequest;
import com.codesync.auth.dto.request.ResetPasswordRequest;
import com.codesync.auth.dto.request.UpdateProfileRequest;
import com.codesync.auth.dto.request.VerifyOtpRequest;
import com.codesync.auth.dto.response.AuthResponse;
import com.codesync.auth.dto.response.MessageResponse;
import com.codesync.auth.dto.response.UserResponse;
import com.codesync.auth.exception.UnauthorizedException;
import com.codesync.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.client.registration.github.client-id:}")
    private String githubClientId;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<MessageResponse> validateOtp(@Valid @RequestBody com.codesync.auth.dto.request.ValidateOtpRequest request) {
        return ResponseEntity.ok(authService.validateOtp(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<MessageResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(authService.resendOtp(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/oauth-login")
    public ResponseEntity<AuthResponse> oauthLogin(@Valid @RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(authService.oauthLogin(request));
    }

    @GetMapping("/oauth2/authorize/{provider}")
    public ResponseEntity<MessageResponse> authorizeRedirect(@PathVariable String provider) {
        if ("google".equalsIgnoreCase(provider)
                && (googleClientId == null || googleClientId.isBlank())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("google OAuth is not configured on the server"));
        }
        if ("github".equalsIgnoreCase(provider)
                && (githubClientId == null || githubClientId.isBlank())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("github OAuth is not configured on the server"));
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/oauth2/authorization/" + provider)
                .build();
    }

    @PostMapping("/oauth-register")
    public ResponseEntity<AuthResponse> completeOAuthRegistration(@Valid @RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(authService.oauthLogin(request));
    }

    @GetMapping("/oauth-callback")
    public ResponseEntity<MessageResponse> oauthCallback() {
        return ResponseEntity.ok(new MessageResponse("OAuth callback endpoint ready"));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ResponseEntity.ok(authService.logout(authorizationHeader));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(authService.getUserByEmail(extractEmail(authentication)));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = authService.getUserByEmail(extractEmail(authentication)).getUserId();
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PutMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = authService.getUserByEmail(extractEmail(authentication)).getUserId();
        return ResponseEntity.ok(authService.changePassword(userId, request));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam("query") String query) {
        return ResponseEntity.ok(authService.searchUsers(query));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @PutMapping("/deactivate")
    public ResponseEntity<MessageResponse> deactivateAccount(Authentication authentication) {
        UUID userId = authService.getUserByEmail(extractEmail(authentication)).getUserId();
        return ResponseEntity.ok(authService.deactivateAccount(userId));
    }

    private String extractEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new UnauthorizedException("Authentication is required");
        }
        return authentication.getName();
    }

    // =========================================================================
    // Admin-only endpoints — protected by hasRole("ADMIN") in SecurityConfig
    // =========================================================================

    @GetMapping("/admin/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PutMapping("/admin/users/{userId}/suspend")
    public ResponseEntity<MessageResponse> suspendUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(authService.suspendUserById(userId));
    }

    @DeleteMapping("/admin/users/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(authService.deleteUserById(userId));
    }
}
