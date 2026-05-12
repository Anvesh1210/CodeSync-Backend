package com.codesync.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"app.jwt.secret=short-secret",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.security.oauth2.client.registration.google.client-id=",
		"spring.security.oauth2.client.registration.google.client-secret=",
		"spring.security.oauth2.client.registration.github.client-id=",
		"spring.security.oauth2.client.registration.github.client-secret=",
		"eureka.client.enabled=false"
})
@org.springframework.boot.autoconfigure.ImportAutoConfiguration(exclude = {
		org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class
})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc

class AuthControllerOAuthConfigValidationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void authorizeGoogleShouldReturnBadRequestWhenOAuthIsNotConfigured() throws Exception {
		mockMvc.perform(get("/auth/oauth2/authorize/google"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("google OAuth is not configured on the server"));
	}

	@Test
	void authorizeGithubShouldReturnBadRequestWhenOAuthIsNotConfigured() throws Exception {
		mockMvc.perform(get("/auth/oauth2/authorize/github"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("github OAuth is not configured on the server"));
	}
}
