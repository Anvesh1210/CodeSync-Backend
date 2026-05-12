package com.codesync.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TokenStateServiceTest {

    private TokenStateService tokenStateService;

    @BeforeEach
    void setUp() {
        tokenStateService = new TokenStateService();
    }

    @Test
    void revokeToken() {
        tokenStateService.revokeToken("token");
        assertTrue(tokenStateService.isTokenRevoked("token"));
    }

    @Test
    void isTokenRevoked() {
        assertFalse(tokenStateService.isTokenRevoked("token"));
        tokenStateService.revokeToken("token");
        assertTrue(tokenStateService.isTokenRevoked("token"));
    }

    @Test
    void storeRefreshToken() {
        tokenStateService.storeRefreshToken("user@test.com", "token");
        assertTrue(tokenStateService.isRefreshTokenValidForUser("user@test.com", "token"));
    }

    @Test
    void revokeRefreshToken() {
        tokenStateService.storeRefreshToken("user@test.com", "token");
        tokenStateService.revokeRefreshToken("user@test.com", "token");
        assertFalse(tokenStateService.isRefreshTokenValidForUser("user@test.com", "token"));
        assertTrue(tokenStateService.isTokenRevoked("token"));
    }

    @Test
    void revokeAllRefreshTokens() {
        tokenStateService.storeRefreshToken("user@test.com", "t1");
        tokenStateService.storeRefreshToken("user@test.com", "t2");
        tokenStateService.revokeAllRefreshTokens("user@test.com");
        assertFalse(tokenStateService.isRefreshTokenValidForUser("user@test.com", "t1"));
        assertTrue(tokenStateService.isTokenRevoked("t1"));
        assertTrue(tokenStateService.isTokenRevoked("t2"));
    }
}
