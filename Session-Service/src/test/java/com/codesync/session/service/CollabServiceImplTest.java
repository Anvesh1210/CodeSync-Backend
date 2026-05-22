package com.codesync.session.service;

import com.codesync.session.dto.*;
import com.codesync.session.entity.CollabSession;
import com.codesync.session.entity.Participant;
import com.codesync.session.entity.ParticipantRole;
import com.codesync.session.repository.CollabRepository;
import com.codesync.session.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollabServiceImplTest {

    @Mock
    private CollabRepository collabRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private CollabServiceImpl collabService;

    private UUID sessionId;
    private UUID userId;
    private UUID projectId;
    private CollabSession mockSession;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        mockSession = CollabSession.builder()
                .sessionId(sessionId)
                .projectId(projectId)
                .ownerId(userId)
                .status(CollabSession.SessionStatus.ACTIVE)
                .maxParticipants(10)
                .build();
    }

    @Test
    void createSession_ShouldSucceed() {
        SessionRequest request = SessionRequest.builder()
                .projectId(projectId)
                .ownerId(userId)
                .isPremium(true)
                .build();

        when(collabRepository.save(any(CollabSession.class))).thenReturn(mockSession);

        SessionResponse response = collabService.createSession(request);

        assertNotNull(response);
        assertEquals(sessionId, response.getSessionId());
        verify(participantRepository).save(any(Participant.class));
    }

    @Test
    void joinSession_ShouldSucceed() {
        ParticipantRequest request = new ParticipantRequest();
        request.setUserId(userId);

        when(collabRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));
        when(participantRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());
        when(participantRepository.save(any(Participant.class))).thenAnswer(inv -> inv.getArgument(0));

        ParticipantResponse response = collabService.joinSession(sessionId, request);

        assertNotNull(response);
        verify(messagingTemplate).convertAndSend(anyString(), any(List.class));
    }

    @Test
    void joinSession_ShouldThrow_WhenSessionEnded() {
        mockSession.setStatus(CollabSession.SessionStatus.ENDED);
        when(collabRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));

        assertThrows(RuntimeException.class, () -> collabService.joinSession(sessionId, new ParticipantRequest()));
    }

    @Test
    void joinSession_ShouldThrow_WhenPasswordInvalid() {
        mockSession.setPasswordProtected(true);
        mockSession.setSessionPassword("secret");
        when(collabRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));

        ParticipantRequest request = new ParticipantRequest();
        request.setPassword("wrong");

        assertThrows(RuntimeException.class, () -> collabService.joinSession(sessionId, request));
    }

    @Test
    void joinSession_ShouldThrow_WhenFull() {
        mockSession.setMaxParticipants(1);
        when(collabRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));
        when(participantRepository.countBySessionIdAndLeftAtIsNull(sessionId)).thenReturn(1L);

        assertThrows(RuntimeException.class, () -> collabService.joinSession(sessionId, new ParticipantRequest()));
    }

    @Test
    void endSession_ShouldSucceed() {
        when(collabRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));
        when(collabRepository.save(any(CollabSession.class))).thenReturn(mockSession);

        SessionResponse response = collabService.endSession(sessionId, userId, "ROLE_USER");

        assertEquals(CollabSession.SessionStatus.ENDED, response.getStatus());
    }

    @Test
    void endSession_ShouldThrow_WhenNotAuthorized() {
        when(collabRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));
        UUID randomUser = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> collabService.endSession(sessionId, randomUser, "ROLE_USER"));
    }

    @Test
    void kickParticipant_ShouldSucceed() {
        when(collabRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));
        Participant p = Participant.builder().userId(UUID.randomUUID()).build();
        when(participantRepository.findBySessionIdAndUserId(any(), any())).thenReturn(Optional.of(p));

        collabService.kickParticipant(sessionId, userId, "ROLE_USER", p.getUserId());

        verify(participantRepository).save(any(Participant.class));
    }

    @Test
    void getSessionById_ShouldReturnResponse() {
        when(collabRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));
        SessionResponse response = collabService.getSessionById(sessionId);
        assertNotNull(response);
    }

    @Test
    void getAllActiveSessions_ShouldReturnList() {
        when(collabRepository.findByStatus(CollabSession.SessionStatus.ACTIVE))
                .thenReturn(Collections.singletonList(mockSession));
        List<SessionResponse> results = collabService.getAllActiveSessions();
        assertEquals(1, results.size());
    }

    @Test
    void endSessionAdmin_ShouldSucceed() {
        when(collabRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));
        when(collabRepository.save(any(CollabSession.class))).thenReturn(mockSession);
        
        SessionResponse response = collabService.endSessionAdmin(sessionId);
        assertEquals(CollabSession.SessionStatus.ENDED, response.getStatus());
    }

    @Test
    void getActiveSessionByProject_ShouldReturnSession() {
        when(collabRepository.findByProjectIdAndStatus(projectId, CollabSession.SessionStatus.ACTIVE))
                .thenReturn(Collections.singletonList(mockSession));

        SessionResponse response = collabService.getActiveSessionByProject(projectId);

        assertNotNull(response);
    }
}
