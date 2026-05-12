package com.codesync.website.controller;

import com.codesync.website.client.ProjectServiceClient;
import com.codesync.website.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectServiceClient projectServiceClient;

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getAllProjects() {
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.getAllProjects(), "All projects fetched"));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<Object>> getPublicProjects() {
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.getPublicProjects(), "Public projects fetched"));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Object>> getProjectById(
            @PathVariable String projectId,
            @RequestParam(value = "userId", required = false) String userId) {
        String callerId = null;
        String callerRole = "ROLE_ANONYMOUS";
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            callerId = (String) auth.getCredentials();
            callerRole = auth.getAuthorities().iterator().next().getAuthority();
        }
        
        // Prefer callerId from token if available, otherwise fallback to param
        String effectiveUserId = (callerId != null) ? callerId : userId;
        
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.getProjectById(projectId, effectiveUserId, callerRole), "Project fetched"));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<ApiResponse<Object>> getProjectsByOwner(@PathVariable String ownerId) {
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.getProjectsByOwner(ownerId), "Projects by owner fetched"));
    }

    @GetMapping("/member/{memberUserId}")
    public ResponseEntity<ApiResponse<Object>> getProjectsByMember(@PathVariable String memberUserId) {
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.getProjectsByMember(memberUserId), "Projects by member fetched"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Object>> searchProjects(@RequestParam("query") String query) {
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.searchProjects(query), "Search results fetched"));
    }

    @GetMapping("/language/{language}")
    public ResponseEntity<ApiResponse<Object>> getProjectsByLanguage(@PathVariable String language) {
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.getProjectsByLanguage(language), "Projects by language fetched"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createProject(@RequestBody Object request) {
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.createProject(request), "Project created"));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Object>> updateProject(@PathVariable String projectId, @RequestBody Object request) {
        String callerId = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
        String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.updateProject(projectId, request, callerId, callerRole), "Project updated"));
    }

    @PutMapping("/{projectId}/archive")
    public ResponseEntity<ApiResponse<Object>> archiveProject(@PathVariable String projectId) {
        String callerId = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
        String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.archiveProject(projectId, callerId, callerRole), "Project archived"));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Object>> deleteProject(@PathVariable String projectId) {
        String callerId = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
        String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.deleteProject(projectId, callerId, callerRole), "Project deleted"));
    }

    @PostMapping("/{projectId}/fork")
    public ResponseEntity<ApiResponse<Object>> forkProject(@PathVariable String projectId, @RequestBody Object request) {
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.forkProject(projectId, request), "Project forked"));
    }

    @PutMapping("/{projectId}/star")
    public ResponseEntity<ApiResponse<Object>> starProject(@PathVariable String projectId, @RequestBody Object request) {
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.starProject(projectId, request), "Project starred"));
    }

    @PostMapping("/{projectId}/members/{userId}")
    public ResponseEntity<ApiResponse<Object>> addMember(
            @PathVariable String projectId,
            @PathVariable String userId,
            @RequestParam(defaultValue = "COLLABORATOR") String role) {
        String callerId = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
        String callerRole = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.addMember(projectId, userId, role, callerId, callerRole), "Member added"));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<ApiResponse<Object>> getProjectMembers(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.success(projectServiceClient.getProjectMembers(projectId), "Members fetched"));
    }
}
