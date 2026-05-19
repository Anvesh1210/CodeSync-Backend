package com.codesync.auth.security;

import com.codesync.auth.dto.request.OAuthLoginRequest;
import com.codesync.auth.dto.response.AuthResponse;
import com.codesync.auth.entity.User;
import com.codesync.auth.entity.AuthProvider;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String ATTR_EMAIL = "email";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PICTURE = "picture";
    private static final String ATTR_AVATAR_URL = "avatar_url";
    private static final String ATTR_LOGIN = "login";
    
    private static final String PARAM_ERROR = "error";
    private static final String PARAM_MESSAGE = "message";
    private static final String PARAM_TOKEN = "token";
    private static final String PARAM_REFRESH_TOKEN = "refreshToken";
    private static final String PARAM_PROVIDER = "provider";
    private static final String PARAM_EMAIL = "email";
    private static final String PARAM_NAME = "name";
    private static final String PARAM_AVATAR_URL = "avatarUrl";

    private final ObjectProvider<AuthService> authServiceProvider;
    private final UserRepository userRepository;

    @Value("${app.oauth2.frontend-redirect-url:http://localhost:4200}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String registrationId = extractRegistrationId(request);
            AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));

            String email = extractAttribute(oAuth2User, ATTR_EMAIL, ATTR_LOGIN);
            String name = extractAttribute(oAuth2User, ATTR_NAME, ATTR_LOGIN);
            String pictureUrl = Optional.ofNullable((String) oAuth2User.getAttribute(ATTR_PICTURE))
                    .orElse(oAuth2User.getAttribute(ATTR_AVATAR_URL));

            if (!StringUtils.hasText(email)) {
                throw new IllegalArgumentException("Email is missing from OAuth provider response");
            }

            String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
            if (!normalizedEmail.contains("@")) {
                normalizedEmail = normalizedEmail + "@" + registrationId.toLowerCase(Locale.ROOT) + ".com";
            }
            String normalizedName = StringUtils.hasText(name) ? name : normalizedEmail.split("@")[0];

            log.info("OAuth2 authentication successful for provider: {}, email: {}", registrationId, normalizedEmail);

            Optional<User> existingUserOptional = userRepository.findByEmail(normalizedEmail);
            if (existingUserOptional.isPresent()) {
                handleExistingUser(request, response, existingUserOptional.get(), provider, normalizedEmail, normalizedName, registrationId);
            } else {
                handleNewUser(request, response, normalizedEmail, normalizedName, pictureUrl, registrationId);
            }

        } catch (Exception e) {
            log.error("OAuth2 authentication failed", e);
            sendErrorRedirect(request, response, "oauth_failed", e.getMessage());
        }
    }

    private String extractAttribute(OAuth2User oAuth2User, String primary, String fallback) {
        String value = oAuth2User.getAttribute(primary);
        if (!StringUtils.hasText(value)) {
            value = oAuth2User.getAttribute(fallback);
        }
        return value;
    }

    private void handleExistingUser(HttpServletRequest request, HttpServletResponse response, User existingUser, 
                                  AuthProvider provider, String email, String name, String registrationId) throws IOException {

        AuthResponse authResponse = authServiceProvider.getObject().oauthLogin(OAuthLoginRequest.builder()
                .provider(provider)
                .email(email)
                .fullName(name)
                .username(existingUser.getUsername())
                .avatarUrl(existingUser.getAvatarUrl())
                .bio(existingUser.getBio())
                .build());

        String callbackUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl + "/auth/oauth-callback")
                .queryParam(PARAM_TOKEN, authResponse.getAccessToken())
                .queryParam(PARAM_REFRESH_TOKEN, authResponse.getRefreshToken())
                .queryParam(PARAM_PROVIDER, registrationId)
                .build().toUriString();
        getRedirectStrategy().sendRedirect(request, response, callbackUrl);
    }

    private void handleNewUser(HttpServletRequest request, HttpServletResponse response, String email, 
                             String name, String pictureUrl, String registrationId) throws IOException {
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl + "/auth/oauth-register")
                .queryParam(PARAM_EMAIL, email)
                .queryParam(PARAM_NAME, name)
                .queryParam(PARAM_AVATAR_URL, pictureUrl)
                .queryParam(PARAM_PROVIDER, registrationId)
                .build().toUriString();

        log.info("Redirecting to OAuth registration page: {}/auth/oauth-register", frontendRedirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private void sendErrorRedirect(HttpServletRequest request, HttpServletResponse response, String error, String message) throws IOException {
        String errorUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl + "/auth/login")
                .queryParam(PARAM_ERROR, error)
                .queryParam(PARAM_MESSAGE, message)
                .build().toUriString();
        getRedirectStrategy().sendRedirect(request, response, errorUrl);
    }

    private String extractRegistrationId(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) return "GOOGLE";
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
