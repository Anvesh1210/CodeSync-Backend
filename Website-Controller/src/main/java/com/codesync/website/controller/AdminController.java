package com.codesync.website.controller;

import com.codesync.website.dto.ApiResponse;
import com.codesync.website.dto.NotificationDto;
import com.codesync.website.dto.UserDto;
import com.codesync.website.service.AdminIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/web/admin")
public class AdminController {

    @Autowired
    private AdminIntegrationService adminIntegrationService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<String>> adminDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Admin Dashboard", "Success"));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Object>> manageAllUsers() {
        return ResponseEntity.ok(adminIntegrationService.manageAllUsers());
    }

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<ApiResponse<UserDto>> suspendUser(@PathVariable String id) {
        log.info("Admin action: suspending user [{}]", id);
        return ResponseEntity.ok(adminIntegrationService.suspendUser(id));
    }

    @DeleteMapping("/users/{id}/delete")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable String id) {
        log.info("Admin action: deleting user [{}]", id);
        return ResponseEntity.ok(adminIntegrationService.deleteUser(id));
    }

    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<Object>> manageAllProjects() {
        return ResponseEntity.ok(adminIntegrationService.manageAllProjects());
    }

    @DeleteMapping("/projects/{id}/delete")
    public ResponseEntity<ApiResponse<String>> deleteProject(@PathVariable String id) {
        log.info("Admin action: deleting project [{}]", id);
        return ResponseEntity.ok(adminIntegrationService.deleteProject(id));
    }

    @GetMapping("/executions")
    public ResponseEntity<ApiResponse<Object>> viewAllExecutions() {
        return ResponseEntity.ok(adminIntegrationService.viewAllExecutions());
    }

    @PostMapping("/executions/{id}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelExecution(@PathVariable String id) {
        log.info("Admin action: cancelling execution [{}]", id);
        return ResponseEntity.ok(adminIntegrationService.cancelExecution(id));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Object>> viewPlatformAnalytics() {
        return ResponseEntity.ok(adminIntegrationService.viewPlatformAnalytics());
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<ApiResponse<Object>> viewActiveCollabSessions() {
        return ResponseEntity.ok(adminIntegrationService.viewActiveCollabSessions());
    }

    @PostMapping("/sessions/{id}/end")
    public ResponseEntity<ApiResponse<String>> endSession(@PathVariable String id) {
        log.info("Admin action: terminating session [{}]", id);
        return ResponseEntity.ok(adminIntegrationService.endSession(id));
    }

    @PostMapping("/notifications/send")
    public ResponseEntity<ApiResponse<NotificationDto>> sendPlatformNotification(@RequestBody NotificationDto notification) {
        return ResponseEntity.ok(adminIntegrationService.sendPlatformNotification(notification));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Object>> viewAuditLogs() {
        return ResponseEntity.ok(adminIntegrationService.viewAuditLogs());
    }

    @GetMapping("/languages")
    public ResponseEntity<ApiResponse<Object>> manageSupportedLanguages() {
        return ResponseEntity.ok(adminIntegrationService.manageSupportedLanguages());
    }
}
