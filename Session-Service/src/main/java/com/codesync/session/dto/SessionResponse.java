package com.codesync.session.dto;

import com.codesync.session.entity.CollabSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    private UUID sessionId;
    private UUID projectId;
    private UUID fileId;
    private UUID ownerId;
    private CollabSession.SessionStatus status;
    private Integer maxParticipants;
    private boolean isPasswordProtected;
    private LocalDateTime createdAt;
    private LocalDateTime endedAt;
}
