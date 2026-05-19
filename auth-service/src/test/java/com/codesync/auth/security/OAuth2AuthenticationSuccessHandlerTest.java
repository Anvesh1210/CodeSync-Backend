package com.codesync.auth.security;

import com.codesync.auth.dto.response.AuthResponse;
import com.codesync.auth.entity.User;
import com.codesync.auth.entity.AuthProvider;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private ObjectProvider<AuthService> authServiceProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @Mock
    private RedirectStrategy redirectStrategy;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(successHandler, "frontendRedirectUrl", "http://localhost:4200");
        successHandler.setRedirectStrategy(redirectStrategy);
    }

    @Test
    void onAuthenticationSuccessForNewUser() throws Exception {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/google");
        when(oAuth2User.getAttribute("email")).thenReturn("new@example.com");
        when(oAuth2User.getAttribute("name")).thenReturn("New User");
        when(oAuth2User.getAttribute("picture")).thenReturn("pic");
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(redirectStrategy).sendRedirect(eq(request), eq(response), contains("/auth/oauth-register"));
    }

    @Test
    void onAuthenticationSuccessForExistingUser() throws Exception {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/google");
        when(oAuth2User.getAttribute("email")).thenReturn("existing@example.com");
        
        User user = User.builder().email("existing@example.com").provider(AuthProvider.GOOGLE).build();
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(user));
        
        AuthService authService = mock(AuthService.class);
        when(authServiceProvider.getObject()).thenReturn(authService);
        when(authService.oauthLogin(any())).thenReturn(AuthResponse.builder().accessToken("at").refreshToken("rt").build());

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(redirectStrategy).sendRedirect(eq(request), eq(response), contains("/auth/oauth-callback"));
    }

    @Test
    void onAuthenticationSuccessWithProviderMismatch() throws Exception {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/google");
        when(oAuth2User.getAttribute("email")).thenReturn("mismatch@example.com");
        
        User user = User.builder().email("mismatch@example.com").provider(AuthProvider.GITHUB).build();
        when(userRepository.findByEmail("mismatch@example.com")).thenReturn(Optional.of(user));

        AuthService authService = mock(AuthService.class);
        when(authServiceProvider.getObject()).thenReturn(authService);
        when(authService.oauthLogin(any())).thenReturn(AuthResponse.builder().accessToken("at").refreshToken("rt").build());

        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Account linking is now enabled, so it should redirect to success callback
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), contains("/auth/oauth-callback"));
    }

    @Test
    void onAuthenticationSuccessWithException() throws Exception {
        when(authentication.getPrincipal()).thenThrow(new RuntimeException("error"));
        successHandler.onAuthenticationSuccess(request, response, authentication);
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), contains("error=oauth_failed"));
    }
}
