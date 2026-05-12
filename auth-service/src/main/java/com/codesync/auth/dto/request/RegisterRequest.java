package com.codesync.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class RegisterRequest {

	@NotBlank(message = "Username is required")
	@Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
	private String username;

	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	@Size(max = 120, message = "Email must be less than 120 characters")
	private String email;

	@NotBlank(message = "Password is required")
	@Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
	private String password;

	@NotBlank(message = "Full name is required")
	@Size(max = 120, message = "Full name must be less than 120 characters")
	private String fullName;

	@Size(max = 512, message = "Avatar URL must be less than 512 characters")
	private String avatarUrl;

	@Size(max = 1000, message = "Bio must be less than 1000 characters")
	private String bio;
}
