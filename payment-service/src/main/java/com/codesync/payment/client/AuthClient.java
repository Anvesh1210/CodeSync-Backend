package com.codesync.payment.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.UUID;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @PostMapping("/auth/premium/update")
    void updatePremiumStatus(@RequestBody PremiumUpdateRequest request);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class PremiumUpdateRequest {
        private UUID userId;
        private boolean isPremium;
        private String planType;
        private LocalDateTime subscriptionStart;
        private LocalDateTime subscriptionExpiry;
    }
}
