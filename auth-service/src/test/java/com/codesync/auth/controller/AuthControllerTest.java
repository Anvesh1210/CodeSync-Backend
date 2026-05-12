package com.codesync.auth.controller;

import com.codesync.auth.entity.AuthProvider;
import com.codesync.auth.dto.request.*;
import com.codesync.auth.dto.response.AuthResponse;
import com.codesync.auth.dto.response.MessageResponse;
import com.codesync.auth.dto.response.UserResponse;
import com.codesync.auth.service.AuthService;
import com.codesync.auth.security.JwtService;
import com.codesync.auth.security.TokenStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private TokenStateService tokenStateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthController authController;

    @Test
    @WithMockUser
    void registerShouldReturnCreated() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("test")
                .email("test@example.com")
                .password("pass1234")
                .fullName("Test User")
                .build();
        when(authService.register(any())).thenReturn(AuthResponse.builder().build());

        mockMvc.perform(post("/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void loginShouldReturnOk() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "pass1234");
        when(authService.login(any())).thenReturn(AuthResponse.builder().build());

        mockMvc.perform(post("/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void verifyOtpShouldReturnOk() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("test@example.com", "123456");
        when(authService.verifyOtp(any())).thenReturn(AuthResponse.builder().build());

        mockMvc.perform(post("/auth/verify-otp").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void authorizeRedirectGoogleNotConfigured() throws Exception {
        ReflectionTestUtils.setField(authController, "googleClientId", "");
        mockMvc.perform(get("/auth/oauth2/authorize/google"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("google OAuth is not configured on the server"));
    }

    @Test
    @WithMockUser
    void authorizeRedirectGoogleSuccess() throws Exception {
        ReflectionTestUtils.setField(authController, "googleClientId", "valid-client-id");
        mockMvc.perform(get("/auth/oauth2/authorize/google"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "/oauth2/authorization/google"));
    }

    @Test
    @WithMockUser
    void authorizeRedirectGithubNotConfigured() throws Exception {
        ReflectionTestUtils.setField(authController, "githubClientId", "");
        mockMvc.perform(get("/auth/oauth2/authorize/github"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("github OAuth is not configured on the server"));
    }

    @Test
    @WithMockUser
    void authorizeRedirectGithubSuccess() throws Exception {
        ReflectionTestUtils.setField(authController, "githubClientId", "valid-client-id");
        mockMvc.perform(get("/auth/oauth2/authorize/github"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "/oauth2/authorization/github"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getProfileShouldReturnOk() throws Exception {
        when(authService.getUserByEmail("test@example.com")).thenReturn(UserResponse.builder().email("test@example.com").build());

        mockMvc.perform(get("/auth/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void updateProfileShouldReturnOk() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("test", "test@example.com", "New Name", null, null);
        when(authService.getUserByEmail("test@example.com")).thenReturn(UserResponse.builder().userId(UUID.randomUUID()).build());
        when(authService.updateProfile(any(), any())).thenReturn(UserResponse.builder().fullName("New Name").build());

        mockMvc.perform(put("/auth/profile").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Name"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void changePasswordShouldReturnOk() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword123", "newPassword123");
        when(authService.getUserByEmail("test@example.com")).thenReturn(UserResponse.builder().userId(UUID.randomUUID()).build());
        when(authService.changePassword(any(), any())).thenReturn(new MessageResponse("success"));

        mockMvc.perform(put("/auth/password").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void searchUsersShouldReturnOk() throws Exception {
        when(authService.searchUsers("query")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/auth/search").param("query", "query"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getUserByIdShouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(authService.getUserById(id)).thenReturn(UserResponse.builder().userId(id).build());

        mockMvc.perform(get("/auth/" + id))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deactivateAccountShouldReturnOk() throws Exception {
        when(authService.getUserByEmail("test@example.com")).thenReturn(UserResponse.builder().userId(UUID.randomUUID()).build());
        when(authService.deactivateAccount(any())).thenReturn(new MessageResponse("success"));

        mockMvc.perform(put("/auth/deactivate").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getAllUsersShouldReturnOk() throws Exception {
        when(authService.getAllUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/auth/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void suspendUserShouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(authService.suspendUserById(id)).thenReturn(new MessageResponse("success"));

        mockMvc.perform(put("/auth/admin/users/" + id + "/suspend").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void deleteUserShouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(authService.deleteUserById(id)).thenReturn(new MessageResponse("success"));

        mockMvc.perform(delete("/auth/admin/users/" + id).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void logoutShouldReturnOk() throws Exception {
        when(authService.logout(anyString())).thenReturn(new MessageResponse("success"));

        mockMvc.perform(post("/auth/logout").with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "anonymousUser")
    void getProfileShouldThrowUnauthorizedWhenAnonymous() throws Exception {
        mockMvc.perform(get("/auth/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfileShouldThrowUnauthorizedWhenNoUser() throws Exception {
        mockMvc.perform(get("/auth/profile"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void oauthCallbackShouldReturnOk() throws Exception {
        mockMvc.perform(get("/auth/oauth-callback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OAuth callback endpoint ready"));
    }

    @Test
    @WithMockUser
    void completeOAuthRegistrationShouldReturnOk() throws Exception {
        OAuthLoginRequest request = OAuthLoginRequest.builder()
                .provider(AuthProvider.GOOGLE)
                .email("test@example.com")
                .fullName("Test User")
                .build();
        when(authService.oauthLogin(any())).thenReturn(AuthResponse.builder().build());

        mockMvc.perform(post("/auth/oauth-register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void resendOtpShouldReturnOk() throws Exception {
        ResendOtpRequest request = new ResendOtpRequest("test@example.com");
        when(authService.resendOtp(any())).thenReturn(new MessageResponse("success"));

        mockMvc.perform(post("/auth/resend-otp").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void forgotPasswordShouldReturnOk() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");
        when(authService.forgotPassword(any())).thenReturn(new MessageResponse("success"));

        mockMvc.perform(post("/auth/forgot-password").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void resetPasswordShouldReturnOk() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("test@example.com", "123456", "pass1234", "pass1234");
        when(authService.resetPassword(any())).thenReturn(new MessageResponse("success"));

        mockMvc.perform(post("/auth/reset-password").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void oauthLoginEndpointShouldReturnOk() throws Exception {
        OAuthLoginRequest request = OAuthLoginRequest.builder()
                .provider(AuthProvider.GOOGLE)
                .email("test@example.com")
                .fullName("Test User")
                .build();
        when(authService.oauthLogin(any())).thenReturn(AuthResponse.builder().build());

        mockMvc.perform(post("/auth/oauth-login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void refreshTokenEndpointShouldReturnOk() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("token");
        when(authService.refreshToken(any())).thenReturn(AuthResponse.builder().build());

        mockMvc.perform(post("/auth/refresh").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
