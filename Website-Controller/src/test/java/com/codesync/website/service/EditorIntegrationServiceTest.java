package com.codesync.website.service;

import com.codesync.website.client.*;
import com.codesync.website.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EditorIntegrationServiceTest {

    @Mock
    private AuthServiceClient authClient;
    @Mock
    private ProjectServiceClient projectClient;
    @Mock
    private SessionServiceClient sessionClient;
    @Mock
    private FileServiceClient fileClient;
    @Mock
    private ExecutionServiceClient executionClient;
    @Mock
    private VersionServiceClient versionClient;
    @Mock
    private NotificationServiceClient notificationClient;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EditorIntegrationService editorIntegrationService;

    private String userId;
    private String projectId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        projectId = UUID.randomUUID().toString();
    }

    private void setupSecurityContext() {
        Authentication auth = mock(Authentication.class);
        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);
        when(auth.getCredentials()).thenReturn(userId);
        org.springframework.security.core.GrantedAuthority authority = () -> "ROLE_USER";
        doReturn(Collections.singletonList(authority)).when(auth).getAuthorities();
    }

    @Test
    void register_ShouldReturnSuccess() {
        when(authClient.register(any())).thenReturn(new Object());
        when(objectMapper.convertValue(any(), eq(AuthResponse.class))).thenReturn(new AuthResponse());
        AuthResponse response = editorIntegrationService.register(new RegisterRequest());
        assertNotNull(response);
    }

    @Test
    void login_ShouldReturnSuccess() {
        when(authClient.login(any())).thenReturn(new Object());
        when(objectMapper.convertValue(any(), eq(AuthResponse.class))).thenReturn(new AuthResponse());
        AuthResponse response = editorIntegrationService.login(new LoginRequest());
        assertNotNull(response);
    }

    @Test
    void startSession_ShouldReturnSuccess() {
        setupSecurityContext();
        ProjectDto projectDto = new ProjectDto();
        projectDto.setOwnerId(userId);
        when(projectClient.getProjectById(anyString(), anyString(), anyString())).thenReturn(new Object());
        when(objectMapper.convertValue(any(), eq(ProjectDto.class))).thenReturn(projectDto);
        when(sessionClient.startSession(any())).thenReturn(new SessionDto());
        ApiResponse<SessionDto> response = editorIntegrationService.startSession(SessionDto.builder().projectId(projectId).ownerId(userId).build());
        assertTrue(response.isSuccess());
    }

    @Test
    void leaveSession_ShouldReturnSuccess() {
        ApiResponse<String> response = editorIntegrationService.leaveSession("session-1", userId);
        assertTrue(response.isSuccess());
    }
}
