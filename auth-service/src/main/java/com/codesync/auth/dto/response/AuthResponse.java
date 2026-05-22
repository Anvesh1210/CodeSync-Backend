package com.codesync.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

	private String accessToken;
	private String refreshToken;
	private String tokenType;
	private long expiresAt;
	private UserResponse user;
	private String message;
}
