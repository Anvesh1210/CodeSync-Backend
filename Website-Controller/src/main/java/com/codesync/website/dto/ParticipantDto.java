package com.codesync.website.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDto {
    private String participantId;
    private String sessionId;
    private String userId;
    private String userName;
    private String role;
    private String color;
    private Integer cursorLine;
    private Integer cursorCol;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
