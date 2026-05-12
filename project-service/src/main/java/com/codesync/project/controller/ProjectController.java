package com.codesync.project.controller;

import com.codesync.project.dto.request.CreateProjectRequest;
import com.codesync.project.dto.request.ForkProjectRequest;
import com.codesync.project.dto.request.StarProjectRequest;
import com.codesync.project.dto.request.UpdateProjectRequest;
import com.codesync.project.dto.response.MessageResponse;
import com.codesync.project.dto.response.ProjectResponse;
import com.codesync.project.service.ProjectService;
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
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management endpoints")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a project")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable UUID projectId,
            @RequestParam(required = false) UUID userId,
            @RequestHeader(value = "X-Caller-Role", required = false, defaultValue = "ROLE_USER") String callerRole) {
        return ResponseEntity.ok(projectService.getProjectById(projectId, userId, callerRole));
    }

    @GetMapping
    @Operation(summary = "Get all projects")
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Get projects by owner")
    public ResponseEntity<List<ProjectResponse>> getProjectsByOwner(@PathVariable UUID ownerId) {
        return ResponseEntity.ok(projectService.getProjectsByOwner(ownerId));
    }

    @GetMapping("/public")
    @Operation(summary = "Get all public projects")
    public ResponseEntity<List<ProjectResponse>> getPublicProjects() {
        return ResponseEntity.ok(projectService.getPublicProjects());
    }

    @GetMapping("/search")
    @Operation(summary = "Search public projects by name")
    public ResponseEntity<List<ProjectResponse>> searchProjects(@RequestParam("query") String query) {
        return ResponseEntity.ok(projectService.searchProjects(query));
    }

    @GetMapping("/member/{memberUserId}")
    @Operation(summary = "Get projects by member")
    public ResponseEntity<List<ProjectResponse>> getProjectsByMember(@PathVariable UUID memberUserId) {
        return ResponseEntity.ok(projectService.getProjectsByMember(memberUserId));
    }

    @GetMapping("/language/{language}")
    @Operation(summary = "Get public projects by language")
    public ResponseEntity<List<ProjectResponse>> getProjectsByLanguage(@PathVariable String language) {
        return ResponseEntity.ok(projectService.getProjectsByLanguage(language));
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Update project metadata")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            @RequestHeader("X-Caller-Id") UUID callerId,
            @RequestHeader("X-Caller-Role") String callerRole) {
        return ResponseEntity.ok(projectService.updateProject(projectId, request, callerId, callerRole));
    }

    @PutMapping("/{projectId}/archive")
    @Operation(summary = "Archive a project")
    public ResponseEntity<ProjectResponse> archiveProject(
            @PathVariable UUID projectId,
            @RequestHeader("X-Caller-Id") UUID callerId,
            @RequestHeader("X-Caller-Role") String callerRole) {
        return ResponseEntity.ok(projectService.archiveProject(projectId, callerId, callerRole));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete a project")
    public ResponseEntity<MessageResponse> deleteProject(
            @PathVariable UUID projectId,
            @RequestHeader("X-Caller-Id") UUID callerId,
            @RequestHeader("X-Caller-Role") String callerRole) {
        projectService.deleteProject(projectId, callerId, callerRole);
        return ResponseEntity.ok(MessageResponse.builder().message("Project deleted successfully").build());
    }

    @PostMapping("/{projectId}/fork")
    @Operation(summary = "Fork a project — creates a new project")
    public ResponseEntity<ProjectResponse> forkProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody ForkProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.forkProject(projectId, request));
    }

    @PutMapping("/{projectId}/star")
    @Operation(summary = "Star a project — increments star count")
    public ResponseEntity<ProjectResponse> starProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody StarProjectRequest request) {
        return ResponseEntity.ok(projectService.starProject(projectId, request.getUserId()));
    }

    @PostMapping("/{projectId}/members/{userId}")
    @Operation(summary = "Add a member to project")
    public ResponseEntity<MessageResponse> addMember(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "COLLABORATOR") com.codesync.project.entity.ProjectMemberRole role,
            @RequestHeader("X-Caller-Id") UUID callerId,
            @RequestHeader("X-Caller-Role") String callerRole) {
        projectService.addMember(projectId, userId, role, callerId, callerRole);
        return ResponseEntity.ok(MessageResponse.builder().message("Member added successfully").build());
    }

    @GetMapping("/{projectId}/members")
    @Operation(summary = "Get project members")
    public ResponseEntity<List<com.codesync.project.entity.ProjectMember>> getProjectMembers(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.getProjectMembers(projectId));
    }
}
