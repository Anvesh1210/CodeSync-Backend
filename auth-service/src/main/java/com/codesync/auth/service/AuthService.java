package com.codesync.auth.service;

import com.codesync.auth.dto.request.ChangePasswordRequest;
import com.codesync.auth.dto.request.ForgotPasswordRequest;
import com.codesync.auth.dto.request.LoginRequest;
import com.codesync.auth.dto.request.OAuthLoginRequest;
import com.codesync.auth.dto.request.PremiumUpdateRequest;
import com.codesync.auth.dto.request.RefreshTokenRequest;
import com.codesync.auth.dto.request.RegisterRequest;
import com.codesync.auth.dto.request.ResendOtpRequest;
import com.codesync.auth.dto.request.ResetPasswordRequest;
import com.codesync.auth.dto.request.UpdateProfileRequest;
import com.codesync.auth.dto.request.VerifyOtpRequest;
import com.codesync.auth.dto.response.AuthResponse;
import com.codesync.auth.dto.response.MessageResponse;
import com.codesync.auth.dto.response.UserResponse;
import java.util.List;
import java.util.UUID;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse verifyOtp(VerifyOtpRequest request);
    MessageResponse validateOtp(com.codesync.auth.dto.request.ValidateOtpRequest request);
    MessageResponse resendOtp(ResendOtpRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse oauthLogin(OAuthLoginRequest request);
    MessageResponse forgotPassword(ForgotPasswordRequest request);
    MessageResponse resetPassword(ResetPasswordRequest request);
    MessageResponse logout(String token);
    UserResponse validateToken(String token);
    AuthResponse refreshToken(RefreshTokenRequest request);
    UserResponse getUserByEmail(String email);
    UserResponse getUserById(UUID userId);
    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);
    MessageResponse changePassword(UUID userId, ChangePasswordRequest request);
    List<UserResponse> searchUsers(String query);
    MessageResponse deactivateAccount(UUID userId);

    // Admin-only operations
    List<UserResponse> getAllUsers();
    MessageResponse suspendUserById(UUID userId);
    MessageResponse deleteUserById(UUID userId);
    UserResponse updatePremiumStatus(PremiumUpdateRequest request);
}
