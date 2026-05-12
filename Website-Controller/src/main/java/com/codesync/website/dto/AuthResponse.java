package com.codesync.website.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresAt;
    private UserDto user;
    private String message;
}
