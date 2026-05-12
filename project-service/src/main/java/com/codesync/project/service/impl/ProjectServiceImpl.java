package com.codesync.project.service.impl;

import com.codesync.project.dto.request.CreateProjectRequest;
import com.codesync.project.dto.request.ForkProjectRequest;
import com.codesync.project.dto.request.UpdateProjectRequest;
import com.codesync.project.dto.response.ProjectResponse;
import com.codesync.project.entity.Project;
import com.codesync.project.entity.ProjectMember;
import com.codesync.project.entity.ProjectMemberRole;
import com.codesync.project.entity.ProjectStar;
import com.codesync.project.entity.ProjectVisibility;
import com.codesync.project.exception.BadRequestException;
import com.codesync.project.exception.NotFoundException;
import com.codesync.project.repository.ProjectMemberRepository;
import com.codesync.project.repository.ProjectRepository;
import com.codesync.project.repository.ProjectStarRepository;
import com.codesync.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectStarRepository projectStarRepository;

    @Override
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        log.info("Creating project '{}' for owner {} | isPremium: {}", request.getName(), request.getOwnerId(), request.isPremium());
        
        // Enforce limits for free users
        if (!request.isPremium() && request.getVisibility() == ProjectVisibility.PRIVATE) {
            long privateCount = projectRepository.findByOwnerId(request.getOwnerId())
                    .stream()
                    .filter(p -> p.getVisibility() == ProjectVisibility.PRIVATE)
                    .count();
            if (privateCount >= 3) {
                throw new BadRequestException("Free users can have at most 3 private projects. Upgrade to Premium for unlimited private projects.");
            }
        }

        Project project = Project.builder()
                .ownerId(request.getOwnerId())
                .name(request.getName())
                .description(request.getDescription())
                .language(request.getLanguage())
                .visibility(request.getVisibility() != null ? request.getVisibility() : ProjectVisibility.PRIVATE)
                .isArchived(false)
                .starCount(0)
                .forkCount(0)
                .build();
        Project saved = projectRepository.save(project);
        addMemberIfAbsent(saved, request.getOwnerId(), ProjectMemberRole.OWNER);
        if (request.getMemberUserIds() != null) {
            request.getMemberUserIds().forEach(userId -> addMemberIfAbsent(saved, userId, ProjectMemberRole.COLLABORATOR));
        }
        return toResponse(saved);
    }

    @Override
    public ProjectResponse getProjectById(UUID projectId, UUID userId, String callerRole) {
        log.info("Fetching project {} with userId context {} and role {}", projectId, userId, callerRole);
        Project project = findProject(projectId);

        if (project.getVisibility() == ProjectVisibility.PRIVATE) {
            boolean isOwner = userId != null && userId.equals(project.getOwnerId());
            boolean isMember = userId != null && projectMemberRepository.existsByProjectProjectIdAndUserId(projectId, userId);

            if (!isOwner && !isMember && !"ROLE_ADMIN".equals(callerRole)) {
                throw new com.codesync.project.exception.ForbiddenException("Access denied to this private project");
            }
        }
        return toResponse(project);
    }

    @Override
    public List<ProjectResponse> getAllProjects() {
        log.info("Fetching all projects");
        return projectRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponse> getProjectsByOwner(UUID ownerId) {
        log.info("Fetching projects for owner {}", ownerId);
        return projectRepository.findByOwnerId(ownerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponse> getPublicProjects() {
        log.info("Fetching all public projects");
        return projectRepository.findByVisibilityAndIsArchived(ProjectVisibility.PUBLIC, false)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponse> searchProjects(String query) {
        log.info("Searching projects with query '{}'", query);
        if (query == null || query.isBlank()) return List.of();
        return projectRepository.searchByName(query.trim())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponse> getProjectsByMember(UUID memberUserId) {
        log.info("Fetching projects for member {}", memberUserId);
        return projectRepository.findByMemberUserId(memberUserId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request, UUID callerId, String callerRole) {
        log.info("Updating project {} by caller {} with role {}", projectId, callerId, callerRole);
        Project project = findProject(projectId);
        checkOwner(project, callerId, callerRole);
        if (request.getName() != null) project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getLanguage() != null) project.setLanguage(request.getLanguage());
        if (request.getVisibility() != null) project.setVisibility(request.getVisibility());
        return toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    public ProjectResponse archiveProject(UUID projectId, UUID callerId, String callerRole) {
        log.info("Archiving project {} by caller {} with role {}", projectId, callerId, callerRole);
        Project project = findProject(projectId);
        checkOwner(project, callerId, callerRole);
        project.setArchived(true);
        return toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    public void deleteProject(UUID projectId, UUID callerId, String callerRole) {
        log.info("Deleting project {} by caller {} with role {}", projectId, callerId, callerRole);
        Project project = findProject(projectId);
        checkOwner(project, callerId, callerRole);
        projectRepository.delete(project);
        projectRepository.flush();
    }

    @Override
    @Transactional
    public ProjectResponse forkProject(UUID projectId, ForkProjectRequest request) {
        log.info("Forking project {} by user {}", projectId, request.getUserId());
        Project source = findProject(projectId);
        if (source.isArchived()) {
            throw new BadRequestException("Cannot fork an archived project");
        }
        String forkName = request.getName() != null ? request.getName() : source.getName() + "-fork";
        Project fork = Project.builder()
                .ownerId(request.getUserId())
                .name(forkName)
                .description(request.getDescription() != null ? request.getDescription() : source.getDescription())
                .language(source.getLanguage())
                .visibility(request.getVisibility() != null ? request.getVisibility() : ProjectVisibility.PRIVATE)
                .sourceProjectId(projectId)
                .isArchived(false)
                .starCount(0)
                .forkCount(0)
                .build();
        Project savedFork = projectRepository.save(fork);
        addMemberIfAbsent(savedFork, request.getUserId(), ProjectMemberRole.OWNER);

        // Increment fork count on source
        source.setForkCount(source.getForkCount() + 1);
        projectRepository.save(source);

        return toResponse(savedFork);
    }

    @Override
    @Transactional
    public ProjectResponse starProject(UUID projectId, UUID userId) {
        log.info("User {} starring project {}", userId, projectId);
        Project project = findProject(projectId);
        if (projectStarRepository.existsByProjectProjectIdAndUserId(projectId, userId)) {
            log.info("Project {} already starred by {}", projectId, userId);
            return toResponse(project);
        }
        ProjectStar star = ProjectStar.builder()
                .project(project)
                .userId(userId)
                .build();
        projectStarRepository.save(star);
        // Increment star count
        project.setStarCount(project.getStarCount() + 1);
        return toResponse(projectRepository.save(project));
    }

    @Override
    public List<ProjectResponse> getProjectsByLanguage(String language) {
        log.info("Fetching public projects by language '{}'", language);
        if (language == null || language.isBlank()) return List.of();
        return projectRepository.findByLanguageIgnoreCaseAndVisibilityAndIsArchived(
                language.trim(), ProjectVisibility.PUBLIC, false)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addMember(UUID projectId, UUID userId, ProjectMemberRole role, UUID callerId, String callerRole) {
        log.info("Adding member {} to project {} by caller {} with role {}", userId, projectId, callerId, callerRole);
        Project project = findProject(projectId);
        
        boolean isSelfJoin = userId.equals(callerId);
        boolean isPublic = project.getVisibility() == ProjectVisibility.PUBLIC;
        
        // Allow if caller is owner/admin OR if it's a self-join on a public project
        if (project.getOwnerId().equals(callerId) || "ROLE_ADMIN".equals(callerRole) || (isSelfJoin && isPublic)) {
            log.info("Authorization successful for adding member. isSelfJoin={}, isPublic={}", isSelfJoin, isPublic);
            addMemberIfAbsent(project, userId, role);
        } else {
            log.warn("Authorization failed for adding member. Owner={}, Caller={}, Role={}, isSelfJoin={}, isPublic={}", 
                project.getOwnerId(), callerId, callerRole, isSelfJoin, isPublic);
            throw new com.codesync.project.exception.ForbiddenException("Only the project owner or admin can add members to a private project");
        }
    }

    @Override
    public List<ProjectMember> getProjectMembers(UUID projectId) {
        log.info("Fetching members for project {}", projectId);
        return projectMemberRepository.findByProjectProjectId(projectId);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Project findProject(UUID projectId) {
        return projectRepository.findByProjectId(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    }

    private void checkOwner(Project project, UUID callerId, String callerRole) {
        if (project.getOwnerId().equals(callerId) || "ROLE_ADMIN".equals(callerRole)) {
            return; // Authorized
        }
        throw new com.codesync.project.exception.ForbiddenException("Only the project owner or admin can perform this action");
    }

    private void addMemberIfAbsent(Project project, UUID userId, ProjectMemberRole role) {
        if (!projectMemberRepository.existsByProjectProjectIdAndUserId(project.getProjectId(), userId)) {
            ProjectMember member = ProjectMember.builder()
                    .project(project)
                    .userId(userId)
                    .role(role)
                    .build();
            projectMemberRepository.save(member);
            log.info("Successfully saved new project member: userId={}, role={} for project={}", userId, role, project.getProjectId());
        } else {
            log.info("User {} is already a member of project {}", userId, project.getProjectId());
        }
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .projectId(project.getProjectId())
                .ownerId(project.getOwnerId())
                .name(project.getName())
                .description(project.getDescription())
                .language(project.getLanguage())
                .visibility(project.getVisibility())
                .isArchived(project.isArchived())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .starCount(project.getStarCount())
                .forkCount(project.getForkCount())
                .sourceProjectId(project.getSourceProjectId())
                .memberUserIds(projectMemberRepository.findByProjectProjectId(project.getProjectId())
                        .stream().map(com.codesync.project.entity.ProjectMember::getUserId).collect(Collectors.toSet()))
                .build();
    }
}
