package com.codesync.auth.dto.request;

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
public class PremiumUpdateRequest {
    private UUID userId;
    private boolean isPremium;
    private String planType;
    private LocalDateTime subscriptionStart;
    private LocalDateTime subscriptionExpiry;
}
