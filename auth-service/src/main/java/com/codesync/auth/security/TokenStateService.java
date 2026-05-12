package com.codesync.auth.security;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class TokenStateService {

	private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();
	private final Map<String, Set<String>> refreshTokensByUser = new ConcurrentHashMap<>();

	public void storeRefreshToken(String email, String refreshToken) {
		refreshTokensByUser.computeIfAbsent(email, key -> ConcurrentHashMap.newKeySet()).add(refreshToken);
	}

	public boolean isRefreshTokenValidForUser(String email, String refreshToken) {
		Set<String> storedTokens = refreshTokensByUser.get(email);
		return storedTokens != null && storedTokens.contains(refreshToken) && !isTokenRevoked(refreshToken);
	}

	public void revokeToken(String token) {
		if (token != null && !token.isBlank()) {
			revokedTokens.add(token);
		}
	}

	public boolean isTokenRevoked(String token) {
		return token != null && revokedTokens.contains(token);
	}

	public void revokeRefreshToken(String email, String refreshToken) {
		Set<String> storedTokens = refreshTokensByUser.get(email);
		if (storedTokens != null) {
			storedTokens.remove(refreshToken);
			if (storedTokens.isEmpty()) {
				refreshTokensByUser.remove(email);
			}
		}
		revokeToken(refreshToken);
	}

	public void revokeAllRefreshTokens(String email) {
		Set<String> tokens = refreshTokensByUser.remove(email);
		if (tokens != null && !tokens.isEmpty()) {
			revokedTokens.addAll(tokens);
		}
	}
}
