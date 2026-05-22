package com.codesync.session.service;

import com.codesync.session.dto.*;
import com.codesync.session.entity.CollabSession;
import com.codesync.session.entity.Participant;
import com.codesync.session.repository.CollabRepository;
import com.codesync.session.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollabServiceImpl implements CollabService {

    private final CollabRepository collabRepository;
    private final ParticipantRepository participantRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public SessionResponse createSession(SessionRequest request) {
        log.info("[CollabService] Creating session for project {} with owner {}", request.getProjectId(), request.getOwnerId());
        int maxParticipants = request.isPremium() ? 10 : 2;
        
        CollabSession session = CollabSession.builder()
                .projectId(request.getProjectId())
                .fileId(request.getFileId())
                .ownerId(request.getOwnerId())
                .projectOwnerId(request.getProjectOwnerId() != null ? request.getProjectOwnerId() : request.getOwnerId())
                .status(CollabSession.SessionStatus.ACTIVE)
                .maxParticipants(maxParticipants)
                .isPasswordProtected(request.isPasswordProtected())
                .sessionPassword(request.getSessionPassword())
                .build();
        
        try {
            CollabSession saved = collabRepository.save(session);
            
            // Auto-join the owner as the first participant
            Participant owner = Participant.builder()
                    .sessionId(saved.getSessionId())
                    .userId(request.getOwnerId())
                    .role(com.codesync.session.entity.ParticipantRole.HOST)
                    .color("#3b82f6") // Default primary blue
                    .build();
            participantRepository.save(owner);
            
            log.info("Session saved successfully with owner joined: {}", saved.getSessionId());
            return toSessionResponse(saved);
        } catch (Exception e) {
            log.error("Failed to save session to database: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public SessionResponse getSessionById(UUID sessionId) {
        log.info("Fetching session {}", sessionId);
        return toSessionResponse(findSession(sessionId));
    }

    @Override
    @Transactional
    public ParticipantResponse joinSession(UUID sessionId, ParticipantRequest request) {
        log.info("User {} joining session {}", request.getUserId(), sessionId);
        CollabSession session = findSession(sessionId);

        if (session.getStatus() == CollabSession.SessionStatus.ENDED) {
            throw new RuntimeException("Session has already ended");
        }
        if (session.isPasswordProtected() &&
                !session.getSessionPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid session password");
        }

        long activeCount = participantRepository.countBySessionIdAndLeftAtIsNull(sessionId);
        if (session.getMaxParticipants() != null && activeCount >= session.getMaxParticipants()) {
            throw new RuntimeException("Session is full");
        }

        // Return existing active participant if already in session
        return participantRepository.findBySessionIdAndUserId(sessionId, request.getUserId())
                .filter(p -> p.getLeftAt() == null)
                .map(p -> {
                    // Even if already active, broadcast to ensure sync
                    broadcastParticipantsUpdate(sessionId);
                    return toParticipantResponse(p);
                })
                .orElseGet(() -> {
                    Participant participant = Participant.builder()
                            .sessionId(sessionId)
                            .userId(request.getUserId())
                            .cursorLine(request.getCursorLine())
                            .cursorCol(request.getCursorCol())
                            .role(request.getRole() != null ? 
                                    com.codesync.session.entity.ParticipantRole.valueOf(request.getRole()) : 
                                    (session.getOwnerId().equals(request.getUserId()) ? 
                                            com.codesync.session.entity.ParticipantRole.HOST : 
                                            com.codesync.session.entity.ParticipantRole.EDITOR))
                            .color(request.getColor() != null ? request.getColor() : "#4A90E2")
                            .build();
                    Participant saved = participantRepository.save(participant);
                    // Notify others via WebSocket with the FULL list of active participants
                    broadcastParticipantsUpdate(sessionId);
                    return toParticipantResponse(saved);
                });
    }

    private void broadcastParticipantsUpdate(UUID sessionId) {
        List<ParticipantResponse> participants = getParticipants(sessionId);
        messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/participants",
                participants);
    }

    @Override
    @Transactional
    public void leaveSession(UUID sessionId, UUID userId) {
        log.info("User {} leaving session {}", userId, sessionId);
        Participant participant = participantRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Participant not found in session"));
        participant.setLeftAt(LocalDateTime.now());
        participantRepository.save(participant);
        broadcastParticipantsUpdate(sessionId);
    }

    @Override
    @Transactional
    public SessionResponse endSession(UUID sessionId, UUID callerId, String callerRole) {
        log.info("[CollabService] Ending session {} by caller {} ({})", sessionId, callerId, callerRole);
        CollabSession session = findSession(sessionId);
        checkSessionOwner(session, callerId, callerRole);
        session.setStatus(CollabSession.SessionStatus.ENDED);
        session.setEndedAt(LocalDateTime.now());

        // Mark all active participants as left
        participantRepository.findBySessionIdAndLeftAtIsNull(sessionId)
                .forEach(p -> {
                    p.setLeftAt(LocalDateTime.now());
                    participantRepository.save(p);
                });

        SessionResponse response = toSessionResponse(collabRepository.save(session));
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/status", response);
        return response;
    }

    @Override
    public List<ParticipantResponse> getParticipants(UUID sessionId) {
        log.info("Fetching active participants for session {}", sessionId);
        return participantRepository.findBySessionIdAndLeftAtIsNull(sessionId)
                .stream().map(this::toParticipantResponse).collect(Collectors.toList());
    }

    @Override
    public List<SessionResponse> getAllActiveSessions() {
        log.info("Fetching all active sessions for admin");
        return collabRepository.findByStatus(CollabSession.SessionStatus.ACTIVE)
                .stream().map(this::toSessionResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SessionResponse endSessionAdmin(UUID sessionId) {
        log.info("Admin ending session {}", sessionId);
        CollabSession session = findSession(sessionId);
        
        session.setStatus(CollabSession.SessionStatus.ENDED);
        session.setEndedAt(LocalDateTime.now());

        // Mark all active participants as left
        participantRepository.findBySessionIdAndLeftAtIsNull(sessionId)
                .forEach(p -> {
                    p.setLeftAt(LocalDateTime.now());
                    participantRepository.save(p);
                });

        SessionResponse response = toSessionResponse(collabRepository.save(session));
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/status", response);
        return response;
    }


    @Override
    @Transactional
    public void kickParticipant(UUID sessionId, UUID callerId, String callerRole, UUID targetUserId) {
        log.info("Kicking user {} from session {} by caller {} with role {}", targetUserId, sessionId, callerId, callerRole);
        CollabSession session = findSession(sessionId);
        checkSessionOwner(session, callerId, callerRole);
        leaveSession(sessionId, targetUserId);
    }

    private void checkSessionOwner(CollabSession session, UUID callerId, String callerRole) {
        log.info("[CheckOwner] DB Owner: {}, Project Owner: {}, Caller ID: {}, Role: {}", 
                session.getOwnerId(), session.getProjectOwnerId(), callerId, callerRole);
        
        boolean isSessionHost = session.getOwnerId().equals(callerId);
        boolean isProjectOwner = session.getProjectOwnerId() != null && session.getProjectOwnerId().equals(callerId);
        
        if (isSessionHost || isProjectOwner || "ROLE_ADMIN".equals(callerRole)) {
            return; // Authorized
        }
        log.warn("[CheckOwner] Authorization FAILED for session {}", session.getSessionId());
        throw new RuntimeException("Only the project owner, session host, or admin can perform this action");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private CollabSession findSession(UUID sessionId) {
        return collabRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    }

    private SessionResponse toSessionResponse(CollabSession s) {
        return SessionResponse.builder()
                .sessionId(s.getSessionId())
                .projectId(s.getProjectId())
                .fileId(s.getFileId())
                .ownerId(s.getOwnerId())
                .status(s.getStatus())
                .maxParticipants(s.getMaxParticipants())
                .isPasswordProtected(s.isPasswordProtected())
                .createdAt(s.getCreatedAt())
                .endedAt(s.getEndedAt())
                .build();
    }

    private ParticipantResponse toParticipantResponse(Participant p) {
        return ParticipantResponse.builder()
                .participantId(p.getParticipantId())
                .sessionId(p.getSessionId())
                .userId(p.getUserId())
                .cursorLine(p.getCursorLine())
                .cursorCol(p.getCursorCol())
                .role(p.getRole())
                .color(p.getColor())
                .joinedAt(p.getJoinedAt())
                .leftAt(p.getLeftAt())
                .build();
    }

    @Override
    public SessionResponse getActiveSessionByProject(UUID projectId) {
        log.info("Checking for active session for project {}", projectId);
        return collabRepository.findByProjectIdAndStatus(projectId, CollabSession.SessionStatus.ACTIVE)
                .stream()
                .findFirst()
                .map(this::toSessionResponse)
                .orElse(null);
    }
}
