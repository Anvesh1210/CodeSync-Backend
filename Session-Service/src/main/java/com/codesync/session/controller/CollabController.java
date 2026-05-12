package com.codesync.session.controller;

import com.codesync.session.dto.*;
import com.codesync.session.service.CollabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
@Tag(name = "Collaboration Sessions", description = "Real-time collaboration session management")
public class CollabController {

    private final CollabService collabService;

    @PostMapping
    @Operation(summary = "Create a new collaboration session")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody SessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(collabService.createSession(request));
    }

    @GetMapping
    @Operation(summary = "Get all active sessions (Admin)")
    public ResponseEntity<List<SessionResponse>> getAllActiveSessions() {
        return ResponseEntity.ok(collabService.getAllActiveSessions());
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session by ID")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(collabService.getSessionById(sessionId));
    }

    @PostMapping("/{sessionId}/join")
    @Operation(summary = "Join a session")
    public ResponseEntity<ParticipantResponse> joinSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ParticipantRequest request) {
        return ResponseEntity.ok(collabService.joinSession(sessionId, request));
    }

    @PostMapping("/{sessionId}/leave")
    @Operation(summary = "Leave a session")
    public ResponseEntity<Void> leaveSession(
            @PathVariable UUID sessionId,
            @RequestParam UUID userId) {
        collabService.leaveSession(sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sessionId}/end")
    @Operation(summary = "End a session — owner only")
    public ResponseEntity<SessionResponse> endSession(
            @PathVariable UUID sessionId,
            @RequestHeader("X-Caller-Id") UUID callerId,
            @RequestHeader("X-Caller-Role") String callerRole) {
        return ResponseEntity.ok(collabService.endSession(sessionId, callerId, callerRole));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Force end a session (Admin)")
    public ResponseEntity<SessionResponse> endSessionAdmin(
            @PathVariable UUID sessionId,
            @RequestHeader("X-Caller-Id") UUID callerId,
            @RequestHeader("X-Caller-Role") String callerRole) {
        return ResponseEntity.ok(collabService.endSession(sessionId, callerId, callerRole));
    }

    @GetMapping("/{sessionId}/participants")
    @Operation(summary = "Get active participants")
    public ResponseEntity<List<ParticipantResponse>> getParticipants(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(collabService.getParticipants(sessionId));
    }


    @PostMapping("/{sessionId}/kick")
    @Operation(summary = "Kick a participant — owner only")
    public ResponseEntity<Void> kickParticipant(
            @PathVariable UUID sessionId,
            @RequestHeader("X-Caller-Id") UUID callerId,
            @RequestHeader("X-Caller-Role") String callerRole,
            @RequestParam UUID targetUserId) {
        collabService.kickParticipant(sessionId, callerId, callerRole, targetUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Find active session for a project")
    public ResponseEntity<SessionResponse> getActiveSessionForProject(@PathVariable UUID projectId) {
        SessionResponse session = collabService.getActiveSessionByProject(projectId);
        if (session == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(session);
    }
}
