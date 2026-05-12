package com.codesync.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Payload sent over WebSocket for real-time code changes.
 * The frontend sends: { fileId, content, userId } via CollaborationService.sendEdit().
 * This DTO must carry all those fields so the broadcast to other users includes full content.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePayload {

    private UUID sessionId;
    private UUID userId;

    // Full file content — the primary real-time sync payload
    private String fileId;
    private String content;

    // Optional delta fields for future OT/CRDT use
    private String type;        // e.g. "INSERT", "DELETE", "REPLACE"
    private Integer fromLine;
    private Integer toLine;
}
