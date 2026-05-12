package com.codesync.website.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @PostMapping("/auth/register")
    Object register(@RequestBody Object user);

    @PostMapping("/auth/login")
    Object login(@RequestBody Object credentials);

    @PostMapping("/auth/verify-otp")
    Object verifyOtp(@RequestBody Object request);

    @PostMapping("/auth/resend-otp")
    Object resendOtp(@RequestBody Object request);

    @PostMapping("/auth/oauth-login")
    Object oauthLogin(@RequestBody Object request);

    @PostMapping("/auth/forgot-password")
    Object forgotPassword(@RequestBody Object request);

    @PostMapping("/auth/reset-password")
    Object resetPassword(@RequestBody Object request);

    @PostMapping("/auth/logout")
    Object logout(@RequestHeader("Authorization") String token);

    @PostMapping("/auth/refresh")
    Object refreshToken(@RequestBody Object request);

    @GetMapping("/auth/search")
    Object searchUsers(@RequestParam("query") String query);

    @GetMapping("/auth/admin/users")
    Object getAllUsers();

    @GetMapping("/auth/profile")
    Object getUserProfile(@RequestHeader("Authorization") String token);

    @PutMapping("/auth/profile")
    Object editProfile(@RequestHeader("Authorization") String token, @RequestBody Object profileDetails);

    @PutMapping("/auth/admin/users/{id}/suspend")
    Object suspendUser(@PathVariable("id") String id);

    @DeleteMapping("/auth/admin/users/{id}")
    Object deleteUser(@PathVariable("id") String id);

    @GetMapping("/auth/{id}")
    Object getUserById(@PathVariable("id") String id);
}
