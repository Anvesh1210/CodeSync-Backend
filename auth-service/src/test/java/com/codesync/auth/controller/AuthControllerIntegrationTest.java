package com.codesync.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SpringBootTest(properties = {
		"app.jwt.secret=short-secret",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.security.oauth2.client.registration.google.client-id=test-google-client-id",
		"spring.security.oauth2.client.registration.google.client-secret=test-google-client-secret",
		"spring.security.oauth2.client.registration.github.client-id=test-github-client-id",
		"spring.security.oauth2.client.registration.github.client-secret=test-github-client-secret",
		"eureka.client.enabled=false"
})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc

class AuthControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;
	
	@MockBean
	private StringRedisTemplate redisTemplate;
	
	@MockBean
	private JavaMailSender mailSender;
	
	@BeforeEach
	void configureRedisMock() {
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
		Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
	}

	@Test
	void registerShouldCreateUserAndReturnPendingVerificationResponse() throws Exception {
		RegisterPayload payload = RegisterPayload.builder()
				.username("anvesh_" + UUID.randomUUID().toString().substring(0, 8))
				.email("anvesh+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
				.password("strongPass123")
				.fullName("Anvesh Kumar")
				.avatarUrl("https://example.com/avatar.jpg")
				.bio("Full stack developer passionate about building scalable web apps.")
				.build();

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "%s",
						  "email": "%s",
						  "password": "%s",
						  "fullName": "%s",
						  "avatarUrl": "%s",
						  "bio": "%s"
						}
						""".formatted(
						payload.getUsername(),
						payload.getEmail(),
						payload.getPassword(),
						payload.getFullName(),
						payload.getAvatarUrl(),
						payload.getBio())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.refreshToken").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.tokenType").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Verification code sent")));
	}
	
	@Test
	void resendOtpShouldBeAccessibleWithoutAuthentication() throws Exception {
		mockMvc.perform(post("/auth/resend-otp")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "missing.user@example.com"
						}
						"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("User not found"));
	}

	@Test
	void oauthAuthorizeGoogleShouldRedirectToOAuthEntryPoint() throws Exception {
		mockMvc.perform(get("/auth/oauth2/authorize/google"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "/oauth2/authorization/google"));
	}

	@Test
	void oauthAuthorizeGithubShouldRedirectToOAuthEntryPoint() throws Exception {
		mockMvc.perform(get("/auth/oauth2/authorize/github"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "/oauth2/authorization/github"));
	}

	@Test
	void openApiDocsShouldBeAccessibleWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.openapi").isNotEmpty());
	}

	@Test
	void oauth2AuthorizationGoogleEndpointShouldRedirectToProvider() throws Exception {
		mockMvc.perform(get("/oauth2/authorization/google"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", org.hamcrest.Matchers.containsString("accounts.google.com")));
	}

	@Test
	void oauth2AuthorizationGithubEndpointShouldRedirectToProvider() throws Exception {
		mockMvc.perform(get("/oauth2/authorization/github"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", org.hamcrest.Matchers.containsString("github.com/login/oauth/authorize")));
	}

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	private static class RegisterPayload {
		private String username;
		private String email;
		private String password;
		private String fullName;
		private String avatarUrl;
		private String bio;
	}
}
