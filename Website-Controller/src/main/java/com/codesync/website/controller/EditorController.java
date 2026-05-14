package com.codesync.website.controller;

import com.codesync.website.dto.ApiResponse;
import com.codesync.website.dto.UserDto;
import com.codesync.website.dto.RegisterRequest;
import com.codesync.website.dto.AuthResponse;
import com.codesync.website.dto.LoginRequest;
import com.codesync.website.dto.UserProfileDto;
import com.codesync.website.dto.ProjectDto;
import com.codesync.website.dto.CodeFileDto;
import com.codesync.website.dto.SessionDto;
import com.codesync.website.dto.ParticipantDto;
import com.codesync.website.dto.ExecutionJobDto;
import com.codesync.website.dto.SnapshotDto;
import com.codesync.website.dto.FileContentRequest;
import com.codesync.website.service.EditorIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/web/app")
public class EditorController {

    @Autowired
    private EditorIntegrationService editorIntegrationService;

    @GetMapping("/home")
    public ResponseEntity<ApiResponse<String>> home() {
        return ResponseEntity.ok(ApiResponse.success("Welcome", "Home reached"));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        log.info("Registering user: {}", request.getEmail());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(editorIntegrationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("Login user: {}", request.getEmail());
        return ResponseEntity.ok(editorIntegrationService.login(request));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<String>> viewDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard data", "Dashboard fetched"));
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<ApiResponse<UserDto>> viewProfile(@PathVariable String id) {
        return ResponseEntity.ok(editorIntegrationService.getUserProfile(id));
    }

    @PutMapping("/profile/{id}")
    public ResponseEntity<ApiResponse<UserDto>> editProfile(@PathVariable String id,
            @RequestBody UserProfileDto profileDetails) {
        return ResponseEntity.ok(editorIntegrationService.editProfile(id, profileDetails));
    }

    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<Object>> searchUsers() {
        return ResponseEntity.ok(editorIntegrationService.searchUsers());
    }

    @GetMapping("/project/{id}")
    public ResponseEntity<ApiResponse<ProjectDto>> viewProject(
            @PathVariable String id,
            @RequestParam(required = false) String userId) {
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }
        String callerId = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
        String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
        
        log.info("[Controller] View project {} by user {}", id, callerId);
        return ResponseEntity.ok(editorIntegrationService.getProject(id, callerId, callerRole));
    }

    @PostMapping("/project/create")
    public ResponseEntity<ApiResponse<ProjectDto>> createProject(@RequestBody ProjectDto project) {
        return ResponseEntity.ok(editorIntegrationService.createProject(project));
    }

    @PostMapping("/project/{id}/fork")
    public ResponseEntity<ApiResponse<ProjectDto>> forkProject(@PathVariable String id,
            @RequestBody ProjectDto projectDetails) {
        return ResponseEntity.ok(editorIntegrationService.forkProject(id, projectDetails));
    }

    @GetMapping("/editor/{projectId}")
    public ResponseEntity<ApiResponse<String>> openEditor(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.success("Editor opened", "Success"));
    }

    @PostMapping("/file/create")
    public ResponseEntity<ApiResponse<CodeFileDto>> createFile(@RequestBody CodeFileDto file) {
        return ResponseEntity.ok(editorIntegrationService.createFile(file));
    }

    @DeleteMapping("/file/{id}/delete")
    public ResponseEntity<ApiResponse<String>> deleteFile(@PathVariable String id, @RequestParam String userId) {
        return ResponseEntity.ok(editorIntegrationService.deleteFile(id, userId));
    }

    @PutMapping("/file/{id}/content")
    public ResponseEntity<ApiResponse<CodeFileDto>> updateFileContent(@PathVariable String id,
            @RequestBody FileContentRequest request) {
        return ResponseEntity
                .ok(editorIntegrationService.updateFileContent(id, request.getContent(), request.getUserId()));
    }

    @PutMapping("/file/{id}/rename")
    public ResponseEntity<ApiResponse<CodeFileDto>> renameFile(@PathVariable String id, @RequestParam String newName,
            @RequestParam String userId) {
        return ResponseEntity.ok(editorIntegrationService.renameFile(id, newName, userId));
    }

    @GetMapping("/file/{id}")
    public ResponseEntity<ApiResponse<CodeFileDto>> getFileById(@PathVariable String id) {
        return ResponseEntity.ok(editorIntegrationService.getFileById(id));
    }

    @GetMapping("/file/tree/{projectId}")
    public ResponseEntity<ApiResponse<Object>> getFileTree(@PathVariable String projectId) {
        return ResponseEntity.ok(editorIntegrationService.getFileTree(projectId));
    }

    @PostMapping("/session/start")
    public ResponseEntity<ApiResponse<SessionDto>> startSession(@RequestBody SessionDto session) {
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null) {
            log.error("[Controller] Authentication missing for startSession");
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }
        if (session.getProjectId() == null || session.getProjectId().isEmpty()) {
            log.error("[Controller] Project ID is missing in startSession request");
            return ResponseEntity.badRequest().body(ApiResponse.error("Project ID is required"));
        }
        String callerId = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
        log.info("[Controller] startSession for project {} from user {}", session.getProjectId(), callerId);
        session.setOwnerId(callerId);
        return ResponseEntity.ok(editorIntegrationService.startSession(session));
    }

    @PostMapping("/session/{id}/join")
    public ResponseEntity<ApiResponse<ParticipantDto>> joinSession(@PathVariable String id, @RequestBody UserDto user) {
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null) {
            log.error("[Controller] Authentication missing for joinSession");
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }
        String callerId = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
        user.setId(callerId);
        log.info("[Controller] Joining session: {} as user: {}", id, callerId);
        return ResponseEntity.ok(editorIntegrationService.joinSession(id, user));
    }

    @PostMapping("/session/{id}/invite")
    public ResponseEntity<ApiResponse<String>> inviteToSession(
            @PathVariable String id,
            @RequestParam String username) {
        log.info("Inviting user {} to session {}", username, id);
        String inviterId = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
        String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
        return ResponseEntity.ok(editorIntegrationService.inviteToSession(id, username, inviterId, callerRole));
    }

    @GetMapping("/session/project/{projectId}")
    public ResponseEntity<ApiResponse<SessionDto>> getActiveSession(@PathVariable String projectId) {
        return ResponseEntity.ok(editorIntegrationService.getActiveSessionForProject(projectId));
    }

    @GetMapping("/session/{id}")
    public ResponseEntity<ApiResponse<SessionDto>> getSession(@PathVariable String id) {
        return ResponseEntity.ok(editorIntegrationService.getSessionById(id));
    }

    @PostMapping("/session/{id}/leave")
    public ResponseEntity<ApiResponse<String>> leaveSession(@PathVariable String id, @RequestParam String userId) {
        return ResponseEntity.ok(editorIntegrationService.leaveSession(id, userId));
    }

    @PostMapping("/session/{id}/end")
    public ResponseEntity<ApiResponse<SessionDto>> endSession(@PathVariable String id) {
        String callerId = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
        String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
        log.info("[Controller] endSession request for {} from {} ({})", id, callerId, callerRole);
        return ResponseEntity.ok(editorIntegrationService.endSession(id, callerId, callerRole));
    }

    @PostMapping("/session/{id}/kick")
    public ResponseEntity<ApiResponse<String>> kickParticipant(
            @PathVariable String id,
            @RequestParam String targetUserId) {
        String callerId = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
        String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
        return ResponseEntity.ok(editorIntegrationService.kickParticipant(id, callerId, callerRole, targetUserId));
    }

    @GetMapping("/session/{id}/participants")
    public ResponseEntity<ApiResponse<java.util.List<ParticipantDto>>> getParticipants(@PathVariable String id) {
        return ResponseEntity.ok(editorIntegrationService.getParticipants(id));
    }


    @PostMapping("/code/run")
    public ResponseEntity<ApiResponse<ExecutionJobDto>> runCode(@RequestBody ExecutionJobDto request) {
        return ResponseEntity.ok(editorIntegrationService.runCode(request));
    }

    @GetMapping("/code/result/{executionId}")
    public ResponseEntity<ApiResponse<ExecutionJobDto>> getResult(@PathVariable String executionId) {
        return ResponseEntity.ok(editorIntegrationService.getResult(executionId));
    }

    @GetMapping("/history/{projectId}")
    public ResponseEntity<ApiResponse<Object>> viewHistory(@PathVariable String projectId) {
        return ResponseEntity.ok(editorIntegrationService.getHistory(projectId));
    }

    @PostMapping("/snapshot/{snapshotId}/restore")
    public ResponseEntity<ApiResponse<SnapshotDto>> restoreSnapshot(@PathVariable String snapshotId) {
        return ResponseEntity.ok(editorIntegrationService.restoreSnapshot(snapshotId));
    }

    @GetMapping("/notifications/{userId}")
    public ResponseEntity<ApiResponse<Object>> viewNotifications(@PathVariable String userId) {
        return ResponseEntity.ok(editorIntegrationService.getNotifications(userId));
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Integer id) {
        return ResponseEntity.ok(editorIntegrationService.markNotificationAsRead(id));
    }

    @PutMapping("/notifications/recipient/{userId}/readAll")
    public ResponseEntity<ApiResponse<String>> markAllRead(@PathVariable String userId) {
        return ResponseEntity.ok(editorIntegrationService.markAllNotificationsAsRead(userId));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logged out", "Success"));
    }

    // WebSocket Methods
    @MessageMapping("/collaboration.events")
    @SendTo("/topic/collaboration")
    public String handleCollaborationEvents(@Payload String event) {
        return event;
    }

    @MessageMapping("/cursor.updates")
    @SendTo("/topic/cursors")
    public String handleCursorUpdates(@Payload String cursorUpdate) {
        return cursorUpdate;
    }

    @MessageMapping("/execution.output")
    @SendTo("/topic/execution")
    public String handleExecutionOutput(@Payload String output) {
        return output;
    }

    @PostMapping("/project/{id}/join")
    public ResponseEntity<ApiResponse<String>> joinProject(@PathVariable String id, @RequestParam String userId) {
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }
        String callerId = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
        String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
        
        // Use callerId from token to ensure it's a self-join
        log.info("[Controller] User {} joining project {}", callerId, id);
        return ResponseEntity.ok(editorIntegrationService.joinProject(id, callerId, callerId, callerRole));
    }

    @GetMapping("/project/{id}/contributors")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, Object>>>> getProjectContributors(
            @PathVariable String id) {
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }
        return ResponseEntity.ok(editorIntegrationService.getProjectContributors(id));
    }
}
