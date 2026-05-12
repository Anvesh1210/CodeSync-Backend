package com.codesync.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Cursor position update sent over WebSocket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPayload {

    private UUID sessionId;
    private UUID userId;
    private String color;
    private Integer cursorLine;
    private Integer cursorCol;
}
