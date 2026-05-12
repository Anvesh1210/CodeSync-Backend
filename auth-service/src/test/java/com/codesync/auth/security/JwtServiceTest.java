package com.codesync.auth.security;

import com.codesync.auth.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;
    private final String secret = "9a4f2c8d3b7a1e6f4d8c2b9a1f4e7d3c6b2a9f1e4d8c7b3a6f9d2c5b8a1f4e7d";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", 86400000L);

        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void generateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userDetails, UserRole.DEVELOPER, userId, true, "PRO");
        
        assertNotNull(token);
        assertEquals("test@example.com", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));
        assertTrue(jwtService.isAccessToken(token));
        assertFalse(jwtService.isRefreshToken(token));
        
        assertEquals(UserRole.DEVELOPER.name(), jwtService.extractClaim(token, claims -> claims.get("role")));
        assertEquals(userId.toString(), jwtService.extractClaim(token, claims -> claims.get("userId")));
        assertEquals(true, jwtService.extractClaim(token, claims -> claims.get("isPremium")));
        assertEquals("PRO", jwtService.extractClaim(token, claims -> claims.get("planType")));
    }

    @Test
    void generateAndValidateRefreshToken() {
        String token = jwtService.generateRefreshToken(userDetails);
        
        assertNotNull(token);
        assertTrue(jwtService.isRefreshToken(token));
        assertFalse(jwtService.isAccessToken(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValidShouldReturnFalseForDifferentUser() {
        String token = jwtService.generateRefreshToken(userDetails);
        UserDetails otherUser = User.builder()
                .username("other@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        
        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void extractClaim() {
        String token = jwtService.generateRefreshToken(userDetails);
        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void getExpirationMs() {
        assertEquals(3600000L, jwtService.getAccessTokenExpirationMs());
        Long refreshExp = (Long) ReflectionTestUtils.getField(jwtService, "refreshTokenExpirationMs");
        assertEquals(86400000L, refreshExp);
    }
}
