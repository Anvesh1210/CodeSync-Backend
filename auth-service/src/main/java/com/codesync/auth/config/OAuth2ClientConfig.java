package com.codesync.auth.config;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

@Configuration
public class OAuth2ClientConfig {

	@Value("${app.oauth2.google.client-id:${spring.security.oauth2.client.registration.google.client-id:}}")
	private String googleClientId;

	@Value("${app.oauth2.google.client-secret:${spring.security.oauth2.client.registration.google.client-secret:}}")
	private String googleClientSecret;

	@Value("${app.oauth2.github.client-id:${spring.security.oauth2.client.registration.github.client-id:}}")
	private String githubClientId;

	@Value("${app.oauth2.github.client-secret:${spring.security.oauth2.client.registration.github.client-secret:}}")
	private String githubClientSecret;

	@Value("${app.oauth2.redirect-base-url:http://localhost:8081}")
	private String oauth2RedirectBaseUrl;

	@Bean
	ClientRegistrationRepository clientRegistrationRepository() {
		Map<String, ClientRegistration> registrations = new LinkedHashMap<>();

		resolveGoogleClientRegistration().ifPresent(registration -> registrations.put("google", registration));
		resolveGithubClientRegistration().ifPresent(registration -> registrations.put("github", registration));

		return new MapBackedClientRegistrationRepository(registrations);
	}

	@Bean
	OAuth2AuthorizedClientService oAuth2AuthorizedClientService(
			ClientRegistrationRepository clientRegistrationRepository) {
		return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
	}

	private Optional<ClientRegistration> resolveGoogleClientRegistration() {
		if (!StringUtils.hasText(googleClientId) || !StringUtils.hasText(googleClientSecret)) {
			return Optional.empty();
		}
		return Optional.of(ClientRegistration.withRegistrationId("google")
				.clientName("Google")
				.clientId(googleClientId)
				.clientSecret(googleClientSecret)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri(resolveRedirectUriTemplate())
				.scope("openid", "profile", "email")
				.issuerUri("https://accounts.google.com")
				.jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
				.authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
				.tokenUri("https://oauth2.googleapis.com/token")
				.userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
				.userNameAttributeName("sub")
				.build());
	}

	private Optional<ClientRegistration> resolveGithubClientRegistration() {
		if (!StringUtils.hasText(githubClientId) || !StringUtils.hasText(githubClientSecret)) {
			return Optional.empty();
		}
		return Optional.of(ClientRegistration.withRegistrationId("github")
				.clientName("GitHub")
				.clientId(githubClientId)
				.clientSecret(githubClientSecret)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri(resolveRedirectUriTemplate())
				.scope("read:user", "user:email")
				.authorizationUri("https://github.com/login/oauth/authorize")
				.tokenUri("https://github.com/login/oauth/access_token")
				.userInfoUri("https://api.github.com/user")
				.userNameAttributeName("id")
				.build());
	}

	private String resolveRedirectUriTemplate() {
		String baseUrl = StringUtils.hasText(oauth2RedirectBaseUrl)
				? oauth2RedirectBaseUrl.trim()
				: "http://localhost:8081";
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		return baseUrl + "/login/oauth2/code/{registrationId}";
	}

	private static class MapBackedClientRegistrationRepository
			implements ClientRegistrationRepository, Iterable<ClientRegistration> {

		private final Map<String, ClientRegistration> registrations;

		private MapBackedClientRegistrationRepository(Map<String, ClientRegistration> registrations) {
			this.registrations = Map.copyOf(registrations);
		}

		@Override
		public ClientRegistration findByRegistrationId(String registrationId) {
			return registrations.get(registrationId);
		}

		@Override
		public java.util.Iterator<ClientRegistration> iterator() {
			Collection<ClientRegistration> values = registrations.values();
			return values.iterator();
		}
	}
}
