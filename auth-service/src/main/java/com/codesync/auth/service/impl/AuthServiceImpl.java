package com.codesync.auth.service.impl;

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
import com.codesync.auth.entity.AuthProvider;
import com.codesync.auth.entity.User;
import com.codesync.auth.entity.UserRole;
import com.codesync.auth.exception.BadRequestException;
import com.codesync.auth.exception.ConflictException;
import com.codesync.auth.exception.NotFoundException;
import com.codesync.auth.exception.UnauthorizedException;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.security.JwtService;
import com.codesync.auth.security.TokenStateService;
import com.codesync.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final TokenStateService tokenStateService;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    
    private static final String ERR_USER_NOT_FOUND = "User not found";
    private static final String ERR_EMAIL_REQUIRED = "Email is required";
    private static final String ERR_INVALID_TOKEN = "Invalid token";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String OTP_KEY_PREFIX = "otp:";
    private static final String PENDING_USER_KEY_PREFIX = "pending_user:";

    @Value("${app.mail.from:no-reply@codesync.local}")
    private String mailFrom = "no-reply@codesync.local";

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email is already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username is already taken");
        }

        // Instead of saving user to DB, we store the registration request in Redis
        try {
            String jsonRequest = objectMapper.writeValueAsString(request);
            redisTemplate.opsForValue().set(PENDING_USER_KEY_PREFIX + normalizedEmail, jsonRequest, 15, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to store pending user in Redis", e);
            throw new BadRequestException("Registration failed, please try again later");
        }
        
        generateAndSendOtp(normalizedEmail);
        
        // Return a response without tokens
        return AuthResponse.builder()
                .message("Verification code sent to " + normalizedEmail)
                .build();
    }

    private void generateAndSendOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        String otp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(buildOtpKey(normalizedEmail), otp, 5, TimeUnit.MINUTES);
        
        // Log the OTP to the console for local development convenience
        log.info("=========================================================");
        log.info("DEVELOPMENT MODE: OTP for {} is: {}", normalizedEmail, otp);
        log.info("=========================================================");
        
        new Thread(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(normalizedEmail);
                message.setSubject("CodeSync Account Verification");
                message.setText("Your OTP for account verification is: " + otp + "\nIt expires in 5 minutes.");
                mailSender.send(message);
            } catch (Exception exception) {
                log.error("Failed to send OTP email to {}", normalizedEmail, exception);
            }
        }).start();
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String key = buildOtpKey(normalizedEmail);
        String savedOtp = redisTemplate.opsForValue().get(key);
        
        if (savedOtp == null || !savedOtp.equals(request.getOtp())) {
            throw new UnauthorizedException("Invalid or expired OTP");
        }
        
        // Check if this is a registration verification
        String pendingUserJson = redisTemplate.opsForValue().get(PENDING_USER_KEY_PREFIX + normalizedEmail);
        if (pendingUserJson != null) {
            try {
                RegisterRequest regRequest = objectMapper.readValue(pendingUserJson, RegisterRequest.class);
                
                User user = User.builder()
                        .username(regRequest.getUsername())
                        .email(normalizedEmail)
                        .passwordHash(passwordEncoder.encode(regRequest.getPassword()))
                        .fullName(regRequest.getFullName())
                        .role(UserRole.DEVELOPER)
                        .provider(AuthProvider.LOCAL)
                        .isActive(true)
                        .avatarUrl(regRequest.getAvatarUrl())
                        .bio(regRequest.getBio())
                        .build();

                User savedUser = userRepository.save(user);
                redisTemplate.delete(PENDING_USER_KEY_PREFIX + normalizedEmail);
                redisTemplate.delete(key);
                
                return generateAuthResponse(savedUser);
            } catch (Exception e) {
                log.error("Failed to process pending registration", e);
                throw new BadRequestException("Verification failed, registration data might have expired");
            }
        }

        // Otherwise, it might be an existing user (e.g. forgot password verification)
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException(ERR_USER_NOT_FOUND));
                
        user.setActive(true);
        userRepository.save(user);
        
        redisTemplate.delete(key);
        
        return generateAuthResponse(user);
    }

    @Override
    public MessageResponse resendOtp(ResendOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (!userRepository.existsByEmail(normalizedEmail)) {
            throw new NotFoundException(ERR_USER_NOT_FOUND);
        }
        generateAndSendOtp(normalizedEmail);
        return new MessageResponse("OTP resent successfully");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword()));
        } catch (BadCredentialsException exception) {
            throw new UnauthorizedException("Invalid email or password");
        }

        tokenStateService.revokeAllRefreshTokens(user.getEmail());
        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse oauthLogin(OAuthLoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(normalizedEmail)
                .map(existingUser -> {
                    if (existingUser.getProvider() != request.getProvider()) {
                        throw new ConflictException("Account exists with " + existingUser.getProvider() + ". Use regular login.");
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    String username = request.getUsername();
                    if (username == null || username.isBlank()) {
                        username = normalizedEmail.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 5);
                    }
                    if (userRepository.existsByUsername(username)) {
                        username = username + "_" + UUID.randomUUID().toString().substring(0, 5);
                    }

                    User newUser = User.builder()
                            .email(normalizedEmail)
                            .username(username)
                            .fullName(request.getFullName())
                            .provider(request.getProvider())
                            .role(UserRole.DEVELOPER)
                            .isActive(true)
                            .avatarUrl(request.getAvatarUrl())
                            .bio(request.getBio())
                            .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password for OAuth users
                            .build();
                    return userRepository.save(newUser);
                });

        tokenStateService.revokeAllRefreshTokens(user.getEmail());
        return generateAuthResponse(user);
    }

    @Override
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (!userRepository.existsByEmail(normalizedEmail)) {
            // We still return success to prevent email enumeration attacks
            return new MessageResponse("If an account exists, an OTP has been sent.");
        }
        generateAndSendOtp(normalizedEmail);
        return new MessageResponse("OTP sent to your email successfully");
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        String normalizedEmail = normalizeEmail(request.getEmail());
        String key = buildOtpKey(normalizedEmail);
        String savedOtp = redisTemplate.opsForValue().get(key);
        
        if (savedOtp == null || !savedOtp.equals(request.getOtp())) {
            throw new UnauthorizedException("Invalid or expired OTP");
        }
        
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException(ERR_USER_NOT_FOUND));
                
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        redisTemplate.delete(key);
        
        return new MessageResponse("Password has been reset successfully");
    }

    @Override
    public MessageResponse logout(String token) {
        if (token != null && token.startsWith(BEARER_PREFIX)) {
            token = token.substring(BEARER_PREFIX.length());
        }
        tokenStateService.revokeToken(token);
        String email = jwtService.extractUsername(token);
        tokenStateService.revokeAllRefreshTokens(email);
        return new MessageResponse("Logged out successfully");
    }

    @Override
    public UserResponse validateToken(String token) {
        if (token != null && token.startsWith(BEARER_PREFIX)) {
            token = token.substring(BEARER_PREFIX.length());
        }
        if (token == null || token.isBlank() || tokenStateService.isTokenRevoked(token)) {
            throw new UnauthorizedException(ERR_INVALID_TOKEN);
        }
        try {
            String email = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (jwtService.isTokenValid(token, userDetails) && jwtService.isAccessToken(token)) {
                User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException(ERR_USER_NOT_FOUND));
                return toResponse(user);
            } else {
                throw new UnauthorizedException(ERR_INVALID_TOKEN);
            }
        } catch (Exception exception) {
            throw new UnauthorizedException(ERR_INVALID_TOKEN);
        }
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }
        if (tokenStateService.isTokenRevoked(refreshToken)) {
            throw new UnauthorizedException("Refresh token is revoked");
        }

        String email;
        try {
            email = jwtService.extractUsername(refreshToken);
        } catch (Exception exception) {
            throw new UnauthorizedException("Refresh token is invalid");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!jwtService.isRefreshToken(refreshToken) || !jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ERR_USER_NOT_FOUND));
        
        tokenStateService.revokeRefreshToken(email, refreshToken);

        return generateAuthResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new NotFoundException(ERR_USER_NOT_FOUND));
        return toResponse(user);
    }

    @Override
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ERR_USER_NOT_FOUND));
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ERR_USER_NOT_FOUND));

        String normalizedRequestedEmail = request.getEmail() == null ? null : normalizeEmail(request.getEmail());
        if (normalizedRequestedEmail != null && !normalizedRequestedEmail.equals(user.getEmail())
                && userRepository.existsByEmail(normalizedRequestedEmail)) {
            throw new ConflictException("Email is already registered");
        }

        if (request.getUsername() != null && !request.getUsername().equalsIgnoreCase(user.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username is already taken");
        }

        if (normalizedRequestedEmail != null) user.setEmail(normalizedRequestedEmail);
        if (request.getUsername() != null) user.setUsername(request.getUsername());
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getBio() != null) user.setBio(request.getBio());

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public MessageResponse changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ERR_USER_NOT_FOUND));
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return new MessageResponse("Password changed successfully");
    }

    @Override
    public List<UserResponse> searchUsers(String query) {
        return userRepository.searchByUsername(query)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MessageResponse deactivateAccount(UUID userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ERR_USER_NOT_FOUND));
        user.setActive(false);
        userRepository.save(user);
        tokenStateService.revokeAllRefreshTokens(user.getEmail());
        return new MessageResponse("Account deactivated successfully");
    }

    // -------------------------------------------------------------------------
    // Admin-only operations
    // -------------------------------------------------------------------------

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MessageResponse suspendUserById(UUID userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
        user.setActive(false);
        userRepository.save(user);
        tokenStateService.revokeAllRefreshTokens(user.getEmail());
        log.info("Admin action: suspended user [{}] ({})", userId, user.getEmail());
        return new MessageResponse("User suspended successfully");
    }

    @Override
    @Transactional
    public MessageResponse deleteUserById(UUID userId) {
        log.info("[Admin] Attempting to delete user with ID: {}", userId);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("[Admin] User not found for deletion: {}", userId);
                    return new NotFoundException("User not found with id: " + userId);
                });
        
        String email = user.getEmail();
        userRepository.deleteById(userId);
        userRepository.flush();
        
        try {
            tokenStateService.revokeAllRefreshTokens(email);
        } catch (Exception e) {
            log.warn("[Admin] Failed to revoke tokens for deleted user {}: {}", email, e.getMessage());
        }
        
        log.info("[Admin] User successfully deleted: {} ({})", userId, email);
        return new MessageResponse("User deleted successfully from database");
    }

    @Override
    @Transactional
    public UserResponse updatePremiumStatus(PremiumUpdateRequest request) {
        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getUserId()));
        
        user.setPremium(request.isPremium());
        user.setPlanType(request.getPlanType());
        user.setSubscriptionStart(request.getSubscriptionStart());
        user.setSubscriptionExpiry(request.getSubscriptionExpiry());
        
        // Update role if user is a standard DEVELOPER
        if (request.isPremium() && user.getRole() == UserRole.DEVELOPER) {
            user.setRole(UserRole.PREMIUM);
        } else if (!request.isPremium() && user.getRole() == UserRole.PREMIUM) {
            user.setRole(UserRole.DEVELOPER);
        }
        
        return toResponse(userRepository.save(user));
    }

    private AuthResponse generateAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails, user.getRole(), user.getUserId(), user.isPremium(), user.getPlanType());
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        tokenStateService.storeRefreshToken(user.getEmail(), refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresAt(Instant.now().plusMillis(jwtService.getAccessTokenExpirationMs()).toEpochMilli())
                .user(toResponse(user))
                .build();
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .provider(user.getProvider())
                .isActive(user.isActive())
                .isPremium(user.isPremium())
                .planType(user.getPlanType())
                .subscriptionStart(user.getSubscriptionStart())
                .subscriptionExpiry(user.getSubscriptionExpiry())
                .createdAt(user.getCreatedAt())
                .bio(user.getBio())
                .build();
    }
    
    private String buildOtpKey(String email) {
        return OTP_KEY_PREFIX + email;
    }
    
    private String normalizeEmail(String email) {
        if (email == null) {
            throw new BadRequestException(ERR_EMAIL_REQUIRED);
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
