package com.codesync.website.service;

import com.codesync.website.client.*;
import com.codesync.website.dto.ApiResponse;
import com.codesync.website.dto.SessionDto;
import com.codesync.website.dto.UserDto;
import com.codesync.website.dto.RegisterRequest;
import com.codesync.website.dto.AuthResponse;
import com.codesync.website.dto.LoginRequest;
import com.codesync.website.dto.UserProfileDto;
import com.codesync.website.dto.ProjectDto;
import com.codesync.website.dto.CodeFileDto;
import com.codesync.website.dto.ParticipantDto;
import com.codesync.website.dto.NotificationDto;
import com.codesync.website.dto.ParticipantRequestDto;
import com.codesync.website.dto.ExecutionJobDto;
import com.codesync.website.dto.SnapshotDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EditorIntegrationService {

    @Autowired
    private AuthServiceClient authClient;

    @Autowired
    private ProjectServiceClient projectClient;

    @Autowired
    private SessionServiceClient sessionClient;

    @Autowired
    private FileServiceClient fileClient;

    @Autowired
    private ExecutionServiceClient executionClient;

    @Autowired
    private VersionServiceClient versionClient;

    @Autowired
    private NotificationServiceClient notificationClient;

    @Autowired
    private ObjectMapper objectMapper;

    public AuthResponse register(RegisterRequest request) {
        log.info("Calling Auth Service for register user: {}", request.getEmail());
        Object response = authClient.register(request);
        return objectMapper.convertValue(response, AuthResponse.class);
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Calling Auth Service for login: {}", request.getEmail());
        Object response = authClient.login(request);
        return objectMapper.convertValue(response, AuthResponse.class);
    }

    public ApiResponse<UserDto> getUserProfile(String id) {
        try {
            return ApiResponse.success((UserDto) authClient.getUserProfile(id), "Profile fetched");
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch profile");
        }
    }

    public ApiResponse<UserDto> editProfile(String id, UserProfileDto profileDetails) {
        try {
            return ApiResponse.success((UserDto) authClient.editProfile(id, profileDetails), "Profile updated");
        } catch (Exception e) {
            return ApiResponse.error("Failed to update profile");
        }
    }

    public ApiResponse<Object> searchUsers() {
        try {
            return ApiResponse.success(authClient.searchUsers(""), "Users fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error("Failed to search users");
        }
    }

    public ApiResponse<ProjectDto> getProject(String id, String userId, String callerRole) {
        try {
            Object response = projectClient.getProjectById(id, userId, callerRole);
            ProjectDto projectDto = objectMapper.convertValue(response, ProjectDto.class);
            enrichProject(projectDto);
            return ApiResponse.success(projectDto, "Project fetched");
        } catch (Exception e) {
            log.error("[Service] Project fetch failed for {}: {}", id, e.getMessage());
            return ApiResponse.error("Project fetch failed");
        }
    }

    public ApiResponse<ProjectDto> createProject(ProjectDto project) {
        try {
            Object response = projectClient.createProject(project);
            ProjectDto projectDto = objectMapper.convertValue(response, ProjectDto.class);
            return ApiResponse.success(projectDto, "Project created");
        } catch (Exception e) {
            return ApiResponse.error("Project creation failed");
        }
    }

    public ApiResponse<ProjectDto> forkProject(String id, ProjectDto projectDetails) {
        try {
            return ApiResponse.success((ProjectDto) projectClient.createProject(projectDetails), "Project forked");
        } catch (Exception e) {
            return ApiResponse.error("Fork failed");
        }
    }

    public ApiResponse<CodeFileDto> createFile(CodeFileDto file) {
        try {
            // Validate write access
            String callerId = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
            String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
            checkProjectWriteAccess(file.getProjectId(), callerId, callerRole);

            CodeFileDto result;
            if (Boolean.TRUE.equals(file.getIsFolder())) {
                result = fileClient.createFolder(file);
            } else {
                result = fileClient.createFile(file);
            }
            return ApiResponse.success(result, "Success");
        } catch (Exception e) {
            log.error("File creation failed for project {} by user {}: {}", file.getProjectId(), file.getUserId(), e.getMessage(), e);
            return ApiResponse.error("File creation failed: " + e.getMessage());
        }
    }

    public ApiResponse<String> deleteFile(String id, String userId) {
        try {
            // 1. Get file to find its project
            CodeFileDto file = fileClient.getFileById(id);
            String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
            
            // 2. Validate write access
            checkProjectWriteAccess(file.getProjectId(), userId, callerRole);

            fileClient.deleteFile(id, userId);
            return ApiResponse.success("Deleted", "File deleted");
        } catch (Exception e) {
            log.error("Delete failed for file {}: {}", id, e.getMessage());
            return ApiResponse.error("Delete failed: " + e.getMessage());
        }
    }

    public ApiResponse<CodeFileDto> updateFileContent(String id, String content, String userId) {
        try {
            // 1. Get file to find its project
            CodeFileDto file = fileClient.getFileById(id);
            String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
            
            // 2. Validate write access
            checkProjectWriteAccess(file.getProjectId(), userId, callerRole);

            com.codesync.website.dto.FileContentRequest request = com.codesync.website.dto.FileContentRequest.builder()
                    .content(content)
                    .userId(userId)
                    .isPremium(true) // Default to true or fetch from user profile if needed
                    .build();
            return ApiResponse.success(fileClient.updateFileContent(id, request), "Content updated");
        } catch (Exception e) {
            log.error("Update failed for file {}: {}", id, e.getMessage());
            return ApiResponse.error("Update failed: " + e.getMessage());
        }
    }

    public ApiResponse<CodeFileDto> renameFile(String id, String newName, String userId) {
        try {
            // 1. Get file to find its project
            CodeFileDto file = fileClient.getFileById(id);
            String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
            
            // 2. Validate write access
            checkProjectWriteAccess(file.getProjectId(), userId, callerRole);

            return ApiResponse.success(fileClient.renameFile(id, newName, userId), "Renamed");
        } catch (Exception e) {
            log.error("Rename failed for file {}: {}", id, e.getMessage());
            return ApiResponse.error("Rename failed: " + e.getMessage());
        }
    }

    public ApiResponse<CodeFileDto> getFileById(String id) {
        try {
            return ApiResponse.success(fileClient.getFileById(id), "File fetched");
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch file");
        }
    }

    public ApiResponse<Object> getFileTree(String projectId) {
        try {
            return ApiResponse.success(fileClient.getFileTree(projectId), "Tree fetched");
        } catch (Exception e) {
            return ApiResponse.error("Tree fetch failed");
        }
    }

    public ApiResponse<SessionDto> startSession(SessionDto session) {
        log.info("Starting session for project: {}, owner: {}", session.getProjectId(), session.getOwnerId());
        try {
            String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
            
            // 1. Fetch project to check write access and get projectOwnerId
            Object projectResponse = projectClient.getProjectById(session.getProjectId(), session.getOwnerId(), callerRole);
            ProjectDto project = objectMapper.convertValue(projectResponse, ProjectDto.class);
            
            // 2. Validate write access to start a session
            checkProjectWriteAccess(session.getProjectId(), session.getOwnerId(), callerRole);

            // 3. Set project owner ID in session request
            session.setProjectOwnerId(project.getOwnerId());

            SessionDto result = sessionClient.startSession(session);
            log.info("Session started successfully: {}", result.getSessionId());
            return ApiResponse.success(result, "Session started");
        } catch (Exception e) {
            log.error("Session start failed for project {}: {}", session.getProjectId(), e.getMessage(), e);
            return ApiResponse.error("Session start failed: " + e.getMessage());
        }
    }

    public ApiResponse<ParticipantDto> joinSession(String id, UserDto user) {
        log.info("User {} joining session {}", user.getId(), id);
        try {
            // 1. Get session to find project
            SessionDto session = sessionClient.getSessionById(id);
            String projectId = session.getProjectId();
            
            // 2. Get caller role (Admin check)
            String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
            
            // 3. Determine role based on project membership
            Object projectResponse = projectClient.getProjectById(projectId, user.getId(), callerRole);
            ProjectDto project = objectMapper.convertValue(projectResponse, ProjectDto.class);
            
            String joinRole = "VIEWER";
            if ("ROLE_ADMIN".equals(callerRole) || user.getId().equals(project.getOwnerId())) {
                joinRole = "HOST";
            } else if (project.getMemberUserIds() != null && project.getMemberUserIds().contains(user.getId())) {
                joinRole = "EDITOR";
            }
            
            log.info("Calculated join role for user {}: {}", user.getId(), joinRole);

            ParticipantRequestDto request = ParticipantRequestDto.builder()
                    .userId(user.getId())
                    .role(joinRole)
                    .build();
            return ApiResponse.success(sessionClient.joinSession(id, request), "Session joined");
        } catch (Exception e) {
            log.error("Session join failed for session {}: {}", id, e.getMessage());
            return ApiResponse.error("Session join failed: " + e.getMessage());
        }
    }

    public ApiResponse<String> inviteToSession(String sessionId, String targetUsername, String inviterId, String callerRole) {
        log.info("User {} inviting {} to session {} with role {}", inviterId, targetUsername, sessionId, callerRole);
        try {
            // 1. Search for target user
            Object searchResult = authClient.searchUsers(targetUsername);
            java.util.List<java.util.Map<String, Object>> users = objectMapper.convertValue(searchResult,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {
                    });

            if (users == null || users.isEmpty()) {
                return ApiResponse.error("User not found: " + targetUsername);
            }

            // Find the exact username match
            java.util.Map<String, Object> targetUser = users.stream()
                    .filter(u -> targetUsername.equals(u.get("username")))
                    .findFirst()
                    .orElse(users.get(0));

            Object recipientIdObj = targetUser.get("userId");
            String recipientId = recipientIdObj != null ? recipientIdObj.toString() : null;

            // 1.5 Fetch additional details for a better notification
            String projectName = "Project";
            String inviterName = "Someone";
            String projectId = "";
            try {
                log.info("Fetching session details for sessionId: {}", sessionId);
                SessionDto session = sessionClient.getSessionById(sessionId);
                if (session != null) {
                    projectId = session.getProjectId();
                    log.info("Found projectId: {} for session: {}", projectId, sessionId);
                    ProjectDto project = (ProjectDto) projectClient.getProjectById(projectId, inviterId, callerRole);
                    if (project != null) {
                        projectName = project.getName();
                    } else {
                        log.warn("Project details not found for projectId: {}", projectId);
                    }
                } else {
                    log.error("Session not found for sessionId: {}", sessionId);
                }

                UserDto inviter = (UserDto) authClient.getUserProfile(inviterId);
                if (inviter != null) {
                    inviterName = inviter.getUsername();
                } else {
                    log.warn("Inviter profile not found for inviterId: {}", inviterId);
                }
            } catch (Exception e) {
                log.error("Error fetching invitation metadata for sessionId {}: {}", sessionId, e.getMessage(), e);
            }

            // 2. Send notification
            String message = String.format("%s invited you to join a collaboration session for project %s", inviterName,
                    projectName);
            String title = projectName + " Invitation";

            NotificationDto notification = new NotificationDto();
            notification.setRecipientId(recipientId);
            notification.setActorId(inviterId);
            notification.setType("INVITATION");
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setRelatedId(sessionId); // sessionId is a UUID string
            notification.setRelatedType(projectId); // Using projectId in relatedType for frontend redirection
                                                    // convenience
            notification.setRead(false);

            notificationClient.sendPlatformNotification(notification);

            return ApiResponse.success("Invitation sent", "Invitation sent successfully to " + targetUsername);
        } catch (Exception e) {
            log.error("Failed to invite user {}: {}", targetUsername, e.getMessage());
            return ApiResponse.error("Failed to send invitation: " + e.getMessage());
        }
    }

    public ApiResponse<String> leaveSession(String sessionId, String userId) {
        try {
            sessionClient.leaveSession(sessionId, userId);
            return ApiResponse.success("Left", "Left session");
        } catch (Exception e) {
            return ApiResponse.error("Failed to leave session");
        }
    }

    public ApiResponse<SessionDto> endSession(String sessionId, String callerId, String callerRole) {
        log.info("[Integration] Attempting to end session {} by caller {} ({})", sessionId, callerId, callerRole);
        try {
            SessionDto result = sessionClient.endSession(sessionId, callerId, callerRole);
            log.info("[Integration] Session {} ended successfully", sessionId);
            return ApiResponse.success(result, "Session ended");
        } catch (Exception e) {
            log.error("End session failed for {}: {}", sessionId, e.getMessage());
            return ApiResponse.error("Failed to end session");
        }
    }

    public ApiResponse<java.util.List<ParticipantDto>> getParticipants(String sessionId) {
        try {
            java.util.List<ParticipantDto> participants = sessionClient.getParticipants(sessionId);

            // Enrich with usernames
            if (participants != null) {
                for (ParticipantDto p : participants) {
                    try {
                        Object userObj = authClient.getUserById(p.getUserId().toString());
                        if (userObj instanceof java.util.Map) {
                            java.util.Map<String, Object> userMap = (java.util.Map<String, Object>) userObj;
                            p.setUserName(userMap.get("username").toString());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to fetch username for participant {}: {}", p.getUserId(), e.getMessage());
                    }
                }
            }
            return ApiResponse.success(participants, "Participants fetched");
        } catch (Exception e) {
            log.error("Fetch participants failed for {}: {}", sessionId, e.getMessage());
            return ApiResponse.error("Failed to fetch participants");
        }
    }

    public ApiResponse<String> kickParticipant(String sessionId, String callerId, String callerRole, String targetUserId) {
        try {
            sessionClient.kickParticipant(sessionId, callerId, callerRole, targetUserId);
            return ApiResponse.success("Kicked", "Participant kicked");
        } catch (Exception e) {
            log.error("Kick participant failed for {}: {}", sessionId, e.getMessage());
            return ApiResponse.error("Failed to kick participant");
        }
    }

    public ApiResponse<SessionDto> getSessionById(String sessionId) {
        try {
            return ApiResponse.success(sessionClient.getSessionById(sessionId), "Session fetched");
        } catch (Exception e) {
            log.error("Fetch session failed for {}: {}", sessionId, e.getMessage());
            return ApiResponse.error("Failed to fetch session");
        }
    }



    public ApiResponse<ExecutionJobDto> runCode(ExecutionJobDto request) {
        try {
            return ApiResponse.success((ExecutionJobDto) executionClient.runCode(request), "Code running");
        } catch (Exception e) {
            return ApiResponse.error("Run failed");
        }
    }

    public ApiResponse<ExecutionJobDto> getResult(String executionId) {
        try {
            return ApiResponse.success((ExecutionJobDto) executionClient.getResult(executionId), "Result fetched");
        } catch (Exception e) {
            return ApiResponse.error("Result fetch failed");
        }
    }

    public ApiResponse<Object> getHistory(String projectId) {
        try {
            return ApiResponse.success(versionClient.getVersionHistory(projectId), "History fetched");
        } catch (Exception e) {
            return ApiResponse.error("History fetch failed");
        }
    }

    public ApiResponse<SnapshotDto> restoreSnapshot(String snapshotId) {
        try {
            return ApiResponse.success((SnapshotDto) versionClient.restoreSnapshot(snapshotId), "Snapshot restored");
        } catch (Exception e) {
            return ApiResponse.error("Restore failed");
        }
    }

    public ApiResponse<Object> getNotifications(String userId) {
        try {
            return ApiResponse.success(notificationClient.getUserNotifications(userId), "Notifications fetched");
        } catch (Exception e) {
            return ApiResponse.error("Fetch failed");
        }
    }

    public ApiResponse<String> markNotificationAsRead(Integer notificationId) {
        try {
            notificationClient.markAsRead(notificationId);
            return ApiResponse.success("Marked as read", "Success");
        } catch (Exception e) {
            return ApiResponse.error("Failed to mark as read");
        }
    }

    public ApiResponse<String> markAllNotificationsAsRead(String userId) {
        try {
            notificationClient.markAllRead(userId);
            return ApiResponse.success("All marked as read", "Success");
        } catch (Exception e) {
            return ApiResponse.error("Failed to mark all as read");
        }
    }

    public ApiResponse<String> joinProject(String projectId, String userId, String callerId, String callerRole) {
        try {
            log.info("[Service] Request to join project: projectId={}, userId={}, callerId={}, callerRole={}", projectId, userId, callerId, callerRole);
            projectClient.addMember(projectId, userId, "COLLABORATOR", callerId, callerRole);
            log.info("[Service] Successfully added user {} to project {}", userId, projectId);
            return ApiResponse.success("Joined project", "Success");
        } catch (Exception e) {
            log.error("Failed to join project {}: {}", projectId, e.getMessage(), e);
            return ApiResponse.error("Failed to join project: " + e.getMessage());
        }
    }

    public ApiResponse<java.util.List<java.util.Map<String, Object>>> getProjectContributors(String projectId) {
        try {
            log.info("[Service] Fetching contributors for project {}", projectId);
            java.util.List<java.util.Map<String, Object>> members = projectClient.getProjectMembers(projectId);
            if (members == null) return ApiResponse.success(new java.util.ArrayList<>(), "No contributors found");

            java.util.List<java.util.Map<String, Object>> enrichedMembers = new java.util.ArrayList<>();

            for (java.util.Map<String, Object> member : members) {
                java.util.Map<String, Object> enriched = new java.util.HashMap<>(member);
                Object userIdObj = member.get("userId");
                if (userIdObj == null) {
                    log.warn("[Service] Member entry missing userId: {}", member);
                    continue;
                }
                String userId = userIdObj.toString();
                
                try {
                    log.debug("[Service] Enriching contributor: {}", userId);
                    Object userObj = authClient.getUserById(userId);
                    
                    if (userObj instanceof java.util.Map) {
                        java.util.Map<String, Object> userMap = (java.util.Map<String, Object>) userObj;
                        
                        // Handle potential ApiResponse wrapper
                        java.util.Map<String, Object> data = userMap;
                        if (userMap.containsKey("data") && userMap.get("data") instanceof java.util.Map) {
                            data = (java.util.Map<String, Object>) userMap.get("data");
                        }
                        
                        enriched.put("username", data.get("username"));
                        enriched.put("fullName", data.get("fullName"));
                        enriched.put("avatarUrl", data.get("avatarUrl"));
                        enriched.put("isPremium", data.get("isPremium"));
                    }
                } catch (Exception e) {
                    log.warn("[Service] Failed to enrich contributor {}: {}", userId, e.getMessage());
                }
                
                // Ensure there's a fallback username if enrichment failed or returned null
                if (enriched.get("username") == null) {
                    enriched.put("username", "User-" + userId.substring(0, 8));
                }
                
                enrichedMembers.add(enriched);
            }
            
            log.info("[Service] Successfully enriched {} contributors", enrichedMembers.size());
            return ApiResponse.success(enrichedMembers, "Contributors fetched");
        } catch (Exception e) {
            log.error("Failed to fetch contributors for project {}: {}", projectId, e.getMessage());
            return ApiResponse.error("Failed to fetch contributors");
        }
    }

    public ApiResponse<SessionDto> getActiveSessionForProject(String projectId) {
        try {
            return ApiResponse.success(sessionClient.getActiveSessionForProject(projectId), "Active session fetched");
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch active session");
        }
    }

    private void checkProjectWriteAccess(String projectId, String userId, String callerRole) {
        if ("ROLE_ADMIN".equals(callerRole)) {
            return;
        }

        try {
            // Fetch project to check ownership and membership
            Object response = projectClient.getProjectById(projectId, userId, callerRole);
            ProjectDto project = objectMapper.convertValue(response, ProjectDto.class);
            
            boolean isOwner = userId != null && userId.equals(project.getOwnerId());
            boolean isMember = false;
            if (project.getMemberUserIds() != null) {
                log.info("[Permission] User: {}, Project Members: {}", userId, project.getMemberUserIds());
                isMember = project.getMemberUserIds().contains(userId);
            }
            
            if (!isOwner && !isMember) {
                log.warn("[Permission] Write access DENIED for user {} on project {}. isOwner={}, isMember={}", 
                    userId, projectId, isOwner, isMember);
                throw new com.codesync.website.exception.ForbiddenException("You do not have permission to modify this project. Join as a contributor first.");
            }
            log.info("[Permission] Write access GRANTED for user {} on project {}", userId, projectId);
        } catch (Exception e) {
            if (e instanceof RuntimeException && e.getMessage().contains("permission")) {
                throw e;
            }
            log.error("Error validating project access for user {} on project {}: {}", userId, projectId, e.getMessage());
            throw new RuntimeException("Failed to validate project permissions");
        }
    }
    public void enrichProjects(java.util.List<ProjectDto> projects) {
        if (projects == null || projects.isEmpty()) return;
        for (ProjectDto project : projects) {
            enrichProject(project);
        }
    }

    private void enrichProject(ProjectDto project) {
        if (project == null) return;

        // Enrich owner
        if (project.getOwnerId() != null) {
            try {
                Object userObj = authClient.getUserById(project.getOwnerId());
                if (userObj instanceof java.util.Map) {
                    java.util.Map<String, Object> userMap = (java.util.Map<String, Object>) userObj;
                    java.util.Map<String, Object> data = userMap;
                    if (userMap.containsKey("data") && userMap.get("data") instanceof java.util.Map) {
                        data = (java.util.Map<String, Object>) userMap.get("data");
                    }
                    project.setOwnerUsername(data.get("username").toString());
                }
            } catch (Exception e) {
                log.warn("Failed to enrich owner {} for project {}", project.getOwnerId(), project.getProjectId());
                project.setOwnerUsername("User-" + project.getOwnerId().substring(0, 8));
            }
        }

        // Enrich members
        if (project.getMemberUserIds() != null && !project.getMemberUserIds().isEmpty()) {
            java.util.List<UserDto> membersList = new java.util.ArrayList<>();
            for (String memberId : project.getMemberUserIds()) {
                try {
                    Object userObj = authClient.getUserById(memberId);
                    if (userObj instanceof java.util.Map) {
                        java.util.Map<String, Object> userMap = (java.util.Map<String, Object>) userObj;
                        java.util.Map<String, Object> data = userMap;
                        if (userMap.containsKey("data") && userMap.get("data") instanceof java.util.Map) {
                            data = (java.util.Map<String, Object>) userMap.get("data");
                        }
                        UserDto userDto = UserDto.builder()
                                .id(data.get("userId").toString())
                                .username(data.get("username").toString())
                                .fullName(data.get("fullName") != null ? data.get("fullName").toString() : null)
                                .avatarUrl(data.get("avatarUrl") != null ? data.get("avatarUrl").toString() : null)
                                .build();
                        membersList.add(userDto);
                    }
                } catch (Exception e) {
                    log.warn("Failed to enrich member {} for project {}", memberId, project.getProjectId());
                }
            }
            project.setMembers(membersList);
        }
    }
}
