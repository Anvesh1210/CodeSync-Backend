package com.codesync.website.service;

import com.codesync.website.client.*;
import com.codesync.website.dto.ApiResponse;
import com.codesync.website.dto.NotificationDto;
import com.codesync.website.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AdminIntegrationService {

    @Autowired
    private AuthServiceClient authClient;

    @Autowired
    private ProjectServiceClient projectClient;

    @Autowired
    private SessionServiceClient sessionClient;

    @Autowired
    private ExecutionServiceClient executionClient;

    @Autowired
    private NotificationServiceClient notificationClient;

    public ApiResponse<Object> manageAllUsers() {
        try {
            return ApiResponse.success(authClient.getAllUsers(), "Users fetched");
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch users");
        }
    }

    public ApiResponse<UserDto> suspendUser(String id) {
        try {
            authClient.suspendUser(id);
            log.info("Admin action: suspended user [{}]", id);
            return ApiResponse.success(null, "User suspended");
        } catch (Exception e) {
            return ApiResponse.error("Failed to suspend user");
        }
    }

    public ApiResponse<String> deleteUser(String id) {
        log.info("[Admin] Service call: deleting user with ID: {}", id);
        try {
            authClient.deleteUser(id);
            log.info("[Admin] User {} successfully deleted via Auth Service", id);
            return ApiResponse.success("Deleted", "User deleted successfully");
        } catch (Exception e) {
            log.error("[Admin] Failed to delete user {}: {}", id, e.getMessage());
            return ApiResponse.error("Failed to delete user: " + e.getMessage());
        }
    }

    public ApiResponse<Object> manageAllProjects() {
        try {
            return ApiResponse.success(projectClient.getAllProjects(), "Projects fetched");
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch projects");
        }
    }

    public ApiResponse<String> deleteProject(String id) {
        try {
            String callerId = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
            String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
            projectClient.deleteProject(id, callerId, callerRole);
            return ApiResponse.success("Deleted", "Project deleted");
        } catch (Exception e) {
            return ApiResponse.error("Failed to delete project");
        }
    }

    public ApiResponse<Object> viewAllExecutions() {
        try {
            return ApiResponse.success(executionClient.getAllExecutions(), "Executions fetched");
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch executions");
        }
    }

    public ApiResponse<String> cancelExecution(String id) {
        try {
            executionClient.cancelExecution(id);
            return ApiResponse.success("Cancelled", "Execution cancelled");
        } catch (Exception e) {
            return ApiResponse.error("Failed to cancel execution");
        }
    }

    public ApiResponse<Object> viewPlatformAnalytics() {
        try {
            return ApiResponse.success(executionClient.getExecutionStats(), "Analytics fetched");
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch analytics");
        }
    }

    public ApiResponse<Object> viewActiveCollabSessions() {
        try {
            return ApiResponse.success(sessionClient.getActiveSessions(), "Sessions fetched");
        } catch (Exception e) {
            return ApiResponse.error("Failed to fetch sessions");
        }
    }

    public ApiResponse<String> endSession(String id) {
        try {
            String callerId = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
            String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
            sessionClient.endSessionAdmin(id, callerId, callerRole);
            return ApiResponse.success("Ended", "Session ended");
        } catch (Exception e) {
            return ApiResponse.error("Failed to end session");
        }
    }

    public ApiResponse<NotificationDto> sendPlatformNotification(NotificationDto notification) {
        try {
            return ApiResponse.success((NotificationDto) notificationClient.sendPlatformNotification(notification), "Notification sent");
        } catch (Exception e) {
            return ApiResponse.error("Failed to send notification");
        }
    }

    public ApiResponse<Object> viewAuditLogs() {
        return ApiResponse.success("Audit Logs", "Logs fetched");
    }

    public ApiResponse<Object> manageSupportedLanguages() {
        return ApiResponse.success("Supported Languages", "Languages fetched");
    }
}
