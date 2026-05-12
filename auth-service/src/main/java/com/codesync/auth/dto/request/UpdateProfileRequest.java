package com.codesync.auth.dto.request;

import jakarta.validation.constraints.Email;
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
public class UpdateProfileRequest {

	@Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
	private String username;

	@Email(message = "Email must be valid")
	@Size(max = 120, message = "Email must be less than 120 characters")
	private String email;

	@Size(max = 120, message = "Full name must be less than 120 characters")
	private String fullName;

	@Size(max = 512, message = "Avatar URL must be less than 512 characters")
	private String avatarUrl;

	@Size(max = 1000, message = "Bio must be less than 1000 characters")
	private String bio;
}
