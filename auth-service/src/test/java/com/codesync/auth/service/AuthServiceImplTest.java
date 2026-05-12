package com.codesync.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.codesync.auth.dto.request.*;
import com.codesync.auth.dto.response.*;
import com.codesync.auth.entity.AuthProvider;
import com.codesync.auth.entity.UserRole;
import com.codesync.auth.exception.ConflictException;
import com.codesync.auth.exception.NotFoundException;
import com.codesync.auth.exception.UnauthorizedException;
import com.codesync.auth.exception.BadRequestException;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.security.JwtService;
import com.codesync.auth.security.TokenStateService;
import com.codesync.auth.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private UserDetailsService userDetailsService;

	@Mock
	private JwtService jwtService;

	@Mock
	private TokenStateService tokenStateService;
	
	@Mock
	private StringRedisTemplate redisTemplate;
	
	@Mock
	private ValueOperations<String, String> valueOperations;
	
	@Mock
	private JavaMailSender mailSender;

	@Mock
	private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

	@InjectMocks
	private AuthServiceImpl authService;

	private UserDetails userDetails;
	private com.codesync.auth.entity.User dbUser;

	@BeforeEach
	void setUp() {
		userDetails = User.builder()
				.username("anvesh@example.com")
				.password("encoded-password")
				.authorities("ROLE_DEVELOPER")
				.build();
				
		dbUser = com.codesync.auth.entity.User.builder()
				.userId(UUID.randomUUID())
				.username("anvesh123")
				.email("anvesh@example.com")
				.passwordHash("encoded-password")
				.fullName("Anvesh Kumar")
				.role(UserRole.DEVELOPER)
				.provider(AuthProvider.LOCAL)
				.isActive(Boolean.TRUE)
				.createdAt(LocalDateTime.now())
				.build();
	}

	@Test
	void registerShouldCreateInactiveUserAndSendOtp() throws Exception {
		RegisterRequest request = RegisterRequest.builder()
				.username("anvesh123")
				.email("Anvesh@Example.com")
				.password("strongPass123")
				.fullName("Anvesh Kumar")
				.build();

		when(userRepository.existsByEmail("anvesh@example.com")).thenReturn(false);
		when(userRepository.existsByUsername("anvesh123")).thenReturn(false);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

		AuthResponse response = authService.register(request);

		assertThat(response.getMessage()).contains("Verification code sent");
		verify(valueOperations).set(eq("pending_user:anvesh@example.com"), anyString(), eq(15L), eq(TimeUnit.MINUTES));
		verify(valueOperations).set(eq("otp:anvesh@example.com"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
	}

	@Test
	void registerShouldThrowConflictWhenEmailAlreadyExists() {
		RegisterRequest request = RegisterRequest.builder().username("anvesh123").email("anvesh@example.com").password("strongPass123").build();
		when(userRepository.existsByEmail("anvesh@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Email is already registered");
	}

	@Test
	void normalizeEmailShouldThrowBadRequestWhenEmailIsNull() {
		assertThatThrownBy(() -> authService.getUserByEmail(null))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Email is required");
	}

	@Test
	void registerShouldThrowConflictWhenUsernameAlreadyExists() {
		RegisterRequest request = RegisterRequest.builder().username("anvesh123").email("anvesh@example.com").password("strongPass123").build();
		when(userRepository.existsByEmail("anvesh@example.com")).thenReturn(false);
		when(userRepository.existsByUsername("anvesh123")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Username is already taken");
	}

	@Test
	void verifyOtpShouldActivateUserAndReturnTokens() {
		VerifyOtpRequest request = new VerifyOtpRequest("anvesh@example.com", "123456");
		dbUser.setActive(false);
		
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("otp:anvesh@example.com")).thenReturn("123456");
		when(userRepository.findByEmail("anvesh@example.com")).thenReturn(Optional.of(dbUser));
		
		when(userDetailsService.loadUserByUsername("anvesh@example.com")).thenReturn(userDetails);
		when(jwtService.generateAccessToken(any(), any(), any(), anyBoolean(), any())).thenReturn("access-token");
		when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
		
		AuthResponse response = authService.verifyOtp(request);
		
		assertThat(response.getAccessToken()).isEqualTo("access-token");
		assertThat(dbUser.isActive()).isTrue();
		verify(redisTemplate).delete("otp:anvesh@example.com");
	}

	@Test
	void verifyOtpShouldThrowUnauthorizedWhenOtpIsInvalid() {
		VerifyOtpRequest request = new VerifyOtpRequest("anvesh@example.com", "123456");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("otp:anvesh@example.com")).thenReturn("wrong");
		
		assertThatThrownBy(() -> authService.verifyOtp(request))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void verifyOtpShouldThrowNotFoundWhenUserMissing() {
		VerifyOtpRequest request = new VerifyOtpRequest("anvesh@example.com", "123456");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("otp:anvesh@example.com")).thenReturn("123456");
		when(userRepository.findByEmail("anvesh@example.com")).thenReturn(Optional.empty());
		
		assertThatThrownBy(() -> authService.verifyOtp(request))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void loginShouldReturnTokensWhenCredentialsAreValid() {
		LoginRequest request = new LoginRequest("anvesh@example.com", "strongPass123");
		
		when(userRepository.findByEmail("anvesh@example.com")).thenReturn(Optional.of(dbUser));
		when(userDetailsService.loadUserByUsername("anvesh@example.com")).thenReturn(userDetails);
		when(jwtService.generateAccessToken(any(), any(), any(), anyBoolean(), any())).thenReturn("access-token");
		
		AuthResponse response = authService.login(request);
		
		assertThat(response.getAccessToken()).isEqualTo("access-token");
		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(tokenStateService).revokeAllRefreshTokens("anvesh@example.com");
	}

	@Test
	void loginShouldThrowUnauthorizedOnBadCredentials() {
		LoginRequest request = new LoginRequest("anvesh@example.com", "wrong");
		when(userRepository.findByEmail("anvesh@example.com")).thenReturn(Optional.of(dbUser));
		doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any());
		
		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void loginShouldThrowUnauthorizedWhenUserIsInactive() {
		LoginRequest request = new LoginRequest("anvesh@example.com", "strongPass123");
		dbUser.setActive(false);
		when(userRepository.findByEmail("anvesh@example.com")).thenReturn(Optional.of(dbUser));
		
		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void oauthLoginShouldCreateGithubUserWhenNotExisting() {
		OAuthLoginRequest request = OAuthLoginRequest.builder()
				.provider(AuthProvider.GITHUB)
				.email("octocat@github.local")
				.username("octocat")
				.build();

		com.codesync.auth.entity.User savedUser = com.codesync.auth.entity.User.builder()
				.userId(UUID.randomUUID())
				.username("octocat")
				.email("octocat@github.local")
				.provider(AuthProvider.GITHUB)
				.isActive(Boolean.TRUE)
				.build();

		when(userRepository.findByEmail("octocat@github.local")).thenReturn(Optional.empty());
		when(userRepository.existsByUsername("octocat")).thenReturn(false);
		when(passwordEncoder.encode(anyString())).thenReturn("encoded-oauth");
		when(userRepository.save(any())).thenReturn(savedUser);
		when(userDetailsService.loadUserByUsername("octocat@github.local")).thenReturn(userDetails);
		when(jwtService.generateAccessToken(any(), any(), any(), anyBoolean(), any())).thenReturn("oauth-access");
		
		AuthResponse response = authService.oauthLogin(request);

		assertThat(response.getUser().getEmail()).isEqualTo("octocat@github.local");
		assertThat(response.getAccessToken()).isEqualTo("oauth-access");
	}
	
	@Test
	void oauthLoginShouldAutoGenerateUsernameWhenMissing() {
		OAuthLoginRequest request = OAuthLoginRequest.builder()
				.provider(AuthProvider.GITHUB)
				.email("anon@github.local")
				.build();
		com.codesync.auth.entity.User savedUser = com.codesync.auth.entity.User.builder()
				.userId(UUID.randomUUID())
				.username("anon_12345")
				.email("anon@github.local")
				.provider(AuthProvider.GITHUB)
				.isActive(Boolean.TRUE)
				.build();
		when(userRepository.findByEmail("anon@github.local")).thenReturn(Optional.empty());
		when(passwordEncoder.encode(anyString())).thenReturn("encoded");
		when(userRepository.save(any())).thenReturn(savedUser);
		when(userDetailsService.loadUserByUsername("anon@github.local")).thenReturn(userDetails);
		when(jwtService.generateAccessToken(any(), any(), any(), anyBoolean(), any())).thenReturn("tk");
		
		AuthResponse response = authService.oauthLogin(request);
		assertThat(response.getUser().getUsername()).startsWith("anon_");
	}
	
	@Test
	void oauthLoginShouldHandleUsernameCollision() {
		OAuthLoginRequest request = OAuthLoginRequest.builder()
				.provider(AuthProvider.GITHUB)
				.email("collision@github.local")
				.username("taken")
				.build();
		when(userRepository.findByEmail("collision@github.local")).thenReturn(Optional.empty());
		when(userRepository.existsByUsername("taken")).thenReturn(true);
		when(passwordEncoder.encode(anyString())).thenReturn("encoded");
		when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(userDetailsService.loadUserByUsername("collision@github.local")).thenReturn(userDetails);
		when(jwtService.generateAccessToken(any(), any(), any(), anyBoolean(), any())).thenReturn("tk");
		
		AuthResponse response = authService.oauthLogin(request);
		assertThat(response.getUser().getUsername()).startsWith("taken_");
	}
	
	@Test
	void oauthLoginShouldRejectWhenLocalAccountAlreadyExists() {
		OAuthLoginRequest request = OAuthLoginRequest.builder().provider(AuthProvider.GOOGLE).email("anvesh@example.com").build();
		when(userRepository.findByEmail("anvesh@example.com")).thenReturn(Optional.of(dbUser));

		assertThatThrownBy(() -> authService.oauthLogin(request))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void validateTokenShouldReturnUserResponseWhenValid() {
		String token = "Bearer valid-token";
		when(tokenStateService.isTokenRevoked("valid-token")).thenReturn(false);
		when(jwtService.extractUsername("valid-token")).thenReturn("anvesh@example.com");
		when(userDetailsService.loadUserByUsername("anvesh@example.com")).thenReturn(userDetails);
		when(jwtService.isTokenValid("valid-token", userDetails)).thenReturn(true);
		when(jwtService.isAccessToken("valid-token")).thenReturn(true);
		when(userRepository.findByEmail("anvesh@example.com")).thenReturn(Optional.of(dbUser));
		
		UserResponse response = authService.validateToken(token);
		
		assertThat(response.getEmail()).isEqualTo("anvesh@example.com");
	}

	@Test
	void validateTokenShouldThrowUnauthorizedWhenInvalid() {
		String token = "invalid-token";
		when(tokenStateService.isTokenRevoked(token)).thenReturn(true);
		
		assertThatThrownBy(() -> authService.validateToken(token))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void validateTokenShouldThrowUnauthorizedOnException() {
		when(jwtService.extractUsername(anyString())).thenThrow(new RuntimeException("error"));
		assertThatThrownBy(() -> authService.validateToken("Bearer some-token"))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void validateTokenShouldThrowUnauthorizedWhenTokenIsNull() {
		assertThatThrownBy(() -> authService.validateToken(null))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void validateTokenShouldThrowUnauthorizedWhenTokenIsBlank() {
		assertThatThrownBy(() -> authService.validateToken(" "))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void validateTokenShouldThrowUnauthorizedWhenNotAccessToken() {
		when(jwtService.extractUsername(anyString())).thenReturn("test@example.com");
		when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
		when(jwtService.isTokenValid(anyString(), any())).thenReturn(true);
		when(jwtService.isAccessToken(anyString())).thenReturn(false);
		assertThatThrownBy(() -> authService.validateToken("Bearer token"))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void validateTokenShouldThrowUnauthorizedWhenUserNotFoundInRepo() {
		when(jwtService.extractUsername(anyString())).thenReturn("test@example.com");
		when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
		when(jwtService.isTokenValid(anyString(), any())).thenReturn(true);
		when(jwtService.isAccessToken(anyString())).thenReturn(true);
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
		assertThatThrownBy(() -> authService.validateToken("Bearer token"))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void refreshTokenShouldReturnNewTokensWhenValid() {
		RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh");
		when(tokenStateService.isTokenRevoked("valid-refresh")).thenReturn(false);
		when(jwtService.extractUsername("valid-refresh")).thenReturn("anvesh@example.com");
		when(userDetailsService.loadUserByUsername("anvesh@example.com")).thenReturn(userDetails);
		when(jwtService.isRefreshToken("valid-refresh")).thenReturn(true);
		when(jwtService.isTokenValid("valid-refresh", userDetails)).thenReturn(true);
		when(userRepository.findByEmail("anvesh@example.com")).thenReturn(Optional.of(dbUser));
		when(jwtService.generateAccessToken(any(), any(), any(), anyBoolean(), any())).thenReturn("new-access");
		
		AuthResponse response = authService.refreshToken(request);
		
		assertThat(response.getAccessToken()).isEqualTo("new-access");
		verify(tokenStateService).revokeRefreshToken("anvesh@example.com", "valid-refresh");
	}

	@Test
	void logoutShouldRevokeTokens() {
		String token = "Bearer some-token";
		when(jwtService.extractUsername("some-token")).thenReturn("anvesh@example.com");
		
		MessageResponse response = authService.logout(token);
		
		assertThat(response.getMessage()).isEqualTo("Logged out successfully");
		verify(tokenStateService).revokeToken("some-token");
		verify(tokenStateService).revokeAllRefreshTokens("anvesh@example.com");
	}

	@Test
	void resendOtpShouldGenerateAndSendOtp() {
		ResendOtpRequest request = new ResendOtpRequest("anvesh@example.com");
		when(userRepository.existsByEmail("anvesh@example.com")).thenReturn(true);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		
		MessageResponse response = authService.resendOtp(request);
		
		assertThat(response.getMessage()).isEqualTo("OTP resent successfully");
		verify(valueOperations).set(eq("otp:anvesh@example.com"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
	}

	@Test
	void forgotPasswordShouldGenerateAndSendOtp() {
		ForgotPasswordRequest request = new ForgotPasswordRequest("anvesh@example.com");
		when(userRepository.existsByEmail("anvesh@example.com")).thenReturn(true);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		
		MessageResponse response = authService.forgotPassword(request);
		
		assertThat(response.getMessage()).isEqualTo("OTP sent to your email successfully");
	}

	@Test
	void forgotPasswordShouldReturnSuccessEvenIfUserNotFound() {
		ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@example.com");
		when(userRepository.existsByEmail("unknown@example.com")).thenReturn(false);
		MessageResponse response = authService.forgotPassword(request);
		assertThat(response.getMessage()).contains("If an account exists");
	}

	@Test
	void resetPasswordShouldUpdatePasswordWhenOtpIsValid() {
		ResetPasswordRequest request = new ResetPasswordRequest("anvesh@example.com", "123456", "newPass", "newPass");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("otp:anvesh@example.com")).thenReturn("123456");
		when(userRepository.findByEmail("anvesh@example.com")).thenReturn(Optional.of(dbUser));
		when(passwordEncoder.encode("newPass")).thenReturn("encoded-new");
		
		MessageResponse response = authService.resetPassword(request);
		
		assertThat(response.getMessage()).isEqualTo("Password has been reset successfully");
		assertThat(dbUser.getPasswordHash()).isEqualTo("encoded-new");
		verify(redisTemplate).delete("otp:anvesh@example.com");
	}

	@Test
	void resetPasswordShouldThrowBadRequestWhenPasswordsDoNotMatch() {
		ResetPasswordRequest request = new ResetPasswordRequest("anvesh@example.com", "123456", "newPass", "diffPass");
		assertThatThrownBy(() -> authService.resetPassword(request))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void resetPasswordShouldThrowUnauthorizedWhenOtpInvalid() {
		ResetPasswordRequest request = new ResetPasswordRequest("anvesh@example.com", "123456", "p", "p");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("otp:anvesh@example.com")).thenReturn("wrong");
		assertThatThrownBy(() -> authService.resetPassword(request))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void getUserByEmailShouldReturnUserResponse() {
		when(userRepository.findByEmail("anvesh@example.com")).thenReturn(Optional.of(dbUser));
		UserResponse response = authService.getUserByEmail("anvesh@example.com");
		assertThat(response.getEmail()).isEqualTo("anvesh@example.com");
	}

	@Test
	void getUserByIdShouldReturnUserResponse() {
		UUID id = dbUser.getUserId();
		when(userRepository.findByUserId(id)).thenReturn(Optional.of(dbUser));
		UserResponse response = authService.getUserById(id);
		assertThat(response.getUserId()).isEqualTo(id);
	}

	@Test
	void updateProfileShouldUpdateAndReturnUser() {
		UUID id = dbUser.getUserId();
		UpdateProfileRequest request = new UpdateProfileRequest("new_username", "new_email@example.com", "New Name", null, null);
		when(userRepository.findByUserId(id)).thenReturn(Optional.of(dbUser));
		when(userRepository.existsByEmail("new_email@example.com")).thenReturn(false);
		when(userRepository.existsByUsername("new_username")).thenReturn(false);
		when(userRepository.save(any())).thenReturn(dbUser);
		
		UserResponse response = authService.updateProfile(id, request);
		
		assertThat(response.getUsername()).isEqualTo("new_username");
		assertThat(response.getEmail()).isEqualTo("new_email@example.com");
	}

	@Test
	void updateProfileShouldThrowConflictWhenEmailTaken() {
		UUID id = dbUser.getUserId();
		UpdateProfileRequest request = new UpdateProfileRequest(null, "taken@example.com", null, null, null);
		when(userRepository.findByUserId(id)).thenReturn(Optional.of(dbUser));
		when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
		assertThatThrownBy(() -> authService.updateProfile(id, request))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void changePasswordShouldUpdatePasswordWhenCurrentIsCorrect() {
		UUID id = dbUser.getUserId();
		ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass");
		when(userRepository.findByUserId(id)).thenReturn(Optional.of(dbUser));
		when(passwordEncoder.matches("oldPass", dbUser.getPasswordHash())).thenReturn(true);
		when(passwordEncoder.encode("newPass")).thenReturn("encoded-new-pass");
		
		MessageResponse response = authService.changePassword(id, request);
		
		assertThat(response.getMessage()).isEqualTo("Password changed successfully");
		assertThat(dbUser.getPasswordHash()).isEqualTo("encoded-new-pass");
	}

	@Test
	void changePasswordShouldThrowUnauthorizedWhenCurrentIsWrong() {
		UUID id = dbUser.getUserId();
		ChangePasswordRequest request = new ChangePasswordRequest("wrong", "new");
		when(userRepository.findByUserId(id)).thenReturn(Optional.of(dbUser));
		when(passwordEncoder.matches("wrong", dbUser.getPasswordHash())).thenReturn(false);
		assertThatThrownBy(() -> authService.changePassword(id, request))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void searchUsersShouldReturnList() {
		when(userRepository.searchByUsername("anv")).thenReturn(List.of(dbUser));
		List<UserResponse> list = authService.searchUsers("anv");
		assertThat(list).hasSize(1);
	}

	@Test
	void deactivateAccountShouldSetInactive() {
		UUID id = dbUser.getUserId();
		when(userRepository.findByUserId(id)).thenReturn(Optional.of(dbUser));
		MessageResponse response = authService.deactivateAccount(id);
		assertThat(response.getMessage()).isEqualTo("Account deactivated successfully");
		assertThat(dbUser.isActive()).isFalse();
		verify(tokenStateService).revokeAllRefreshTokens(dbUser.getEmail());
	}

	@Test
	void getAllUsersShouldReturnAll() {
		when(userRepository.findAll()).thenReturn(List.of(dbUser));
		List<UserResponse> list = authService.getAllUsers();
		assertThat(list).hasSize(1);
	}

	@Test
	void suspendUserByIdShouldSetInactive() {
		UUID id = dbUser.getUserId();
		when(userRepository.findByUserId(id)).thenReturn(Optional.of(dbUser));
		MessageResponse response = authService.suspendUserById(id);
		assertThat(response.getMessage()).isEqualTo("User suspended successfully");
		assertThat(dbUser.isActive()).isFalse();
	}

	@Test
	void deleteUserByIdShouldDelete() {
		UUID id = dbUser.getUserId();
		when(userRepository.findByUserId(id)).thenReturn(Optional.of(dbUser));
		MessageResponse response = authService.deleteUserById(id);
		assertThat(response.getMessage()).isEqualTo("User deleted successfully from database");
		verify(userRepository).deleteById(id);
	}

	@Test
	void updatePremiumStatusShouldUpdateRole() {
		PremiumUpdateRequest request = new PremiumUpdateRequest(dbUser.getUserId(), true, "PRO", LocalDateTime.now(), LocalDateTime.now().plusDays(30));
		when(userRepository.findByUserId(dbUser.getUserId())).thenReturn(Optional.of(dbUser));
		when(userRepository.save(any())).thenReturn(dbUser);
		
		UserResponse response = authService.updatePremiumStatus(request);
		
		assertThat(dbUser.getRole()).isEqualTo(UserRole.PREMIUM);
		assertThat(dbUser.isPremium()).isTrue();
	}

	@Test
	void updatePremiumStatusShouldDowngradeRole() {
		PremiumUpdateRequest request = new PremiumUpdateRequest(dbUser.getUserId(), false, "FREE", null, null);
		dbUser.setRole(UserRole.PREMIUM);
		when(userRepository.findByUserId(dbUser.getUserId())).thenReturn(Optional.of(dbUser));
		when(userRepository.save(any())).thenReturn(dbUser);
		authService.updatePremiumStatus(request);
		assertThat(dbUser.getRole()).isEqualTo(UserRole.DEVELOPER);
	}

	@Test
	void updatePremiumStatusShouldThrowNotFound() {
		PremiumUpdateRequest request = new PremiumUpdateRequest(UUID.randomUUID(), true, "PRO", null, null);
		when(userRepository.findByUserId(any())).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> authService.updatePremiumStatus(request));
	}

	@Test
	void suspendUserByIdShouldThrowNotFound() {
		UUID id = UUID.randomUUID();
		when(userRepository.findByUserId(any())).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> authService.suspendUserById(id));
	}

	@Test
	void deleteUserByIdShouldThrowNotFound() {
		UUID id = UUID.randomUUID();
		when(userRepository.findByUserId(any())).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> authService.deleteUserById(id));
	}

	@Test
	void updateProfileShouldHandleSameEmailAndUsername() {
		UUID id = dbUser.getUserId();
		UpdateProfileRequest request = new UpdateProfileRequest(dbUser.getUsername(), dbUser.getEmail(), "New Name", null, null);
		when(userRepository.findByUserId(id)).thenReturn(Optional.of(dbUser));
		when(userRepository.save(any())).thenReturn(dbUser);
		UserResponse response = authService.updateProfile(id, request);
		assertThat(response.getFullName()).isEqualTo("New Name");
	}
}
