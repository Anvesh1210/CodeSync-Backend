package com.codesync.project.service;

import com.codesync.project.dto.request.CreateProjectRequest;
import com.codesync.project.dto.request.ForkProjectRequest;
import com.codesync.project.dto.request.UpdateProjectRequest;
import com.codesync.project.dto.response.ProjectResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectService {

    ProjectResponse createProject(CreateProjectRequest request);

    ProjectResponse getProjectById(UUID projectId, UUID userId, String callerRole);

    List<ProjectResponse> getAllProjects();

    List<ProjectResponse> getProjectsByOwner(UUID ownerId);

    List<ProjectResponse> getPublicProjects();

    List<ProjectResponse> searchProjects(String query);

    List<ProjectResponse> getProjectsByMember(UUID memberUserId);

    ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request, UUID callerId, String callerRole);

    ProjectResponse archiveProject(UUID projectId, UUID callerId, String callerRole);

    void deleteProject(UUID projectId, UUID callerId, String callerRole);

    ProjectResponse forkProject(UUID projectId, ForkProjectRequest request);

    ProjectResponse starProject(UUID projectId, UUID userId);

    List<ProjectResponse> getProjectsByLanguage(String language);

    void addMember(UUID projectId, UUID userId, com.codesync.project.entity.ProjectMemberRole role, UUID callerId, String callerRole);

    List<com.codesync.project.entity.ProjectMember> getProjectMembers(UUID projectId);
}
