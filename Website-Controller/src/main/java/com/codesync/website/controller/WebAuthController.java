package com.codesync.website.controller;

import com.codesync.website.client.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/auth")
@RequiredArgsConstructor
public class WebAuthController {

    private final AuthServiceClient authServiceClient;

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody Object user) {
        return ResponseEntity.ok(authServiceClient.register(user));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Object credentials) {
        return ResponseEntity.ok(authServiceClient.login(credentials));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Object> verifyOtp(@RequestBody Object request) {
        return ResponseEntity.ok(authServiceClient.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Object> resendOtp(@RequestBody Object request) {
        return ResponseEntity.ok(authServiceClient.resendOtp(request));
    }

    @PostMapping("/oauth-login")
    public ResponseEntity<Object> oauthLogin(@RequestBody Object request) {
        return ResponseEntity.ok(authServiceClient.oauthLogin(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Object> forgotPassword(@RequestBody Object request) {
        return ResponseEntity.ok(authServiceClient.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Object> resetPassword(@RequestBody Object request) {
        return ResponseEntity.ok(authServiceClient.resetPassword(request));
    }

    @GetMapping("/profile")
    public ResponseEntity<Object> getProfile(
            @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null) {
            return ResponseEntity.status(401).body("Missing Authorization Header");
        }
        return ResponseEntity.ok(authServiceClient.getUserProfile(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(
            @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null) {
            return ResponseEntity.status(401).body("Missing Authorization Header");
        }
        return ResponseEntity.ok(authServiceClient.logout(token));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Object> refresh(@RequestBody Object request) {
        return ResponseEntity.ok(authServiceClient.refreshToken(request));
    }
}
