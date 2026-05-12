package com.codesync.session.service;

import com.codesync.session.dto.*;

import java.util.List;
import java.util.UUID;

public interface CollabService {

    SessionResponse createSession(SessionRequest request);

    SessionResponse getSessionById(UUID sessionId);

    ParticipantResponse joinSession(UUID sessionId, ParticipantRequest request);

    void leaveSession(UUID sessionId, UUID userId);

    SessionResponse endSession(UUID sessionId, UUID callerId, String callerRole);

    List<ParticipantResponse> getParticipants(UUID sessionId);

    List<SessionResponse> getAllActiveSessions();

    SessionResponse endSessionAdmin(UUID sessionId);

    void kickParticipant(UUID sessionId, UUID callerId, String callerRole, UUID targetUserId);
    
    SessionResponse getActiveSessionByProject(UUID projectId);
}
