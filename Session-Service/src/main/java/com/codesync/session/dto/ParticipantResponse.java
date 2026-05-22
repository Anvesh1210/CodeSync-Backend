package com.codesync.session.dto;

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
public class ParticipantResponse {

    private UUID participantId;
    private UUID sessionId;
    private UUID userId;
    private Integer cursorLine;
    private Integer cursorCol;
    private String color;
    private com.codesync.session.entity.ParticipantRole role;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
