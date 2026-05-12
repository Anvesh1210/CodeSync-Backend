package com.codesync.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.codesync.auth.entity.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	public static final String ACCESS_TOKEN_TYPE = "access";
	public static final String REFRESH_TOKEN_TYPE = "refresh";
	
	private static final String CLAIM_TOKEN_TYPE = "token_type";
	private static final String CLAIM_ROLE = "role";
	private static final String CLAIM_USER_ID = "userId";
	private static final String CLAIM_IS_PREMIUM = "isPremium";
	private static final String CLAIM_PLAN_TYPE = "planType";

	@Value("${app.jwt.secret}")
	private String jwtSecret;

	@Value("${app.jwt.access-token-expiration-ms}")
	private long accessTokenExpirationMs;

	@Value("${app.jwt.refresh-token-expiration-ms}")
	private long refreshTokenExpirationMs;

	public String generateAccessToken(UserDetails userDetails, UserRole role, java.util.UUID userId, boolean isPremium, String planType) {
		Map<String, Object> claims = new HashMap<>();
		claims.put(CLAIM_TOKEN_TYPE, ACCESS_TOKEN_TYPE);
		claims.put(CLAIM_ROLE, role.name());
		claims.put(CLAIM_USER_ID, userId.toString());
		claims.put(CLAIM_IS_PREMIUM, isPremium);
		claims.put(CLAIM_PLAN_TYPE, planType);
		return buildToken(claims, userDetails.getUsername(), accessTokenExpirationMs);
	}

	public String generateRefreshToken(UserDetails userDetails) {
		Map<String, Object> claims = new HashMap<>();
		claims.put(CLAIM_TOKEN_TYPE, REFRESH_TOKEN_TYPE);
		return buildToken(claims, userDetails.getUsername(), refreshTokenExpirationMs);
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		String username = extractUsername(token);
		return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
	}

	public boolean isAccessToken(String token) {
		return ACCESS_TOKEN_TYPE.equals(extractTokenType(token));
	}

	public boolean isRefreshToken(String token) {
		return REFRESH_TOKEN_TYPE.equals(extractTokenType(token));
	}

	public long getAccessTokenExpirationMs() {
		return accessTokenExpirationMs;
	}

	private String buildToken(Map<String, Object> claims, String subject, long expirationMs) {
		Instant now = Instant.now();
		return Jwts.builder()
				.claims(claims)
				.subject(subject)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(expirationMs)))
				.signWith(getSigningKey())
				.compact();
	}

	private String extractTokenType(String token) {
		Object tokenType = extractAllClaims(token).get(CLAIM_TOKEN_TYPE);
		return tokenType == null ? null : tokenType.toString();
	}

	private boolean isTokenExpired(String token) {
		Date expiration = extractClaim(token, Claims::getExpiration);
		return expiration.before(new Date());
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private SecretKey getSigningKey() {
		byte[] keyBytes;
		try {
			keyBytes = Decoders.BASE64.decode(jwtSecret);
		} catch (RuntimeException exception) {
			keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
		}
		return Keys.hmacShaKeyFor(ensureMinKeyLength(keyBytes));
	}

	private byte[] ensureMinKeyLength(byte[] keyBytes) {
		if (keyBytes.length >= 32) {
			return keyBytes;
		}
		try {
			return MessageDigest.getInstance("SHA-256").digest(keyBytes);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Unable to initialize JWT key", exception);
		}
	}
}
