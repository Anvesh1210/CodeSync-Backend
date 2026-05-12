package com.codesync.auth.dto.response;

import com.codesync.auth.entity.AuthProvider;
import com.codesync.auth.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID userId;
    private String username;
    private String email;
    private String fullName;
    private UserRole role;
    private String avatarUrl;
    private AuthProvider provider;
    private boolean isActive;
    private boolean isPremium;
    private String planType;
    private LocalDateTime subscriptionStart;
    private LocalDateTime subscriptionExpiry;
    private LocalDateTime createdAt;
    private String bio;
}
