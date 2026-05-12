package com.codesync.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codesync.project.dto.request.CreateProjectRequest;
import com.codesync.project.dto.request.ForkProjectRequest;
import com.codesync.project.dto.request.UpdateProjectRequest;
import com.codesync.project.dto.response.ProjectResponse;
import com.codesync.project.entity.Project;
import com.codesync.project.entity.ProjectMember;
import com.codesync.project.entity.ProjectMemberRole;
import com.codesync.project.entity.ProjectVisibility;
import com.codesync.project.exception.BadRequestException;
import com.codesync.project.exception.ForbiddenException;
import com.codesync.project.exception.NotFoundException;
import com.codesync.project.repository.ProjectMemberRepository;
import com.codesync.project.repository.ProjectRepository;
import com.codesync.project.repository.ProjectStarRepository;
import com.codesync.project.service.impl.ProjectServiceImpl;
import com.codesync.project.entity.ProjectStar;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectStarRepository projectStarRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    // --- createProject Tests ---

    @Test
    void createProjectShouldCreateOwnerAndMembers() {
        UUID ownerId = UUID.randomUUID();
        CreateProjectRequest request = CreateProjectRequest.builder()
                .ownerId(ownerId)
                .name("Realtime Editor")
                .language("Java")
                .visibility(ProjectVisibility.PUBLIC)
                .memberUserIds(new HashSet<>(Collections.singletonList(UUID.randomUUID())))
                .build();

        UUID projectId = UUID.randomUUID();
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            p.setProjectId(projectId);
            return p;
        });

        ProjectResponse response = projectService.createProject(request);

        assertThat(response.getProjectId()).isEqualTo(projectId);
        verify(projectMemberRepository, atLeastOnce()).save(any(ProjectMember.class));
    }

    @Test
    void createProjectShouldUseDefaultVisibilityIfNull() {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .ownerId(UUID.randomUUID()).name("Test").language("Java").visibility(null).build();
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        ProjectResponse response = projectService.createProject(request);
        assertThat(response.getVisibility()).isEqualTo(ProjectVisibility.PRIVATE);
    }

    @Test
    void createProjectShouldEnforcePrivateLimitForFreeUsers() {
        UUID ownerId = UUID.randomUUID();
        CreateProjectRequest request = CreateProjectRequest.builder()
                .ownerId(ownerId)
                .name("Private Project")
                .language("Java")
                .visibility(ProjectVisibility.PRIVATE)
                .isPremium(false)
                .build();

        List<Project> existingPrivate = Arrays.asList(new Project(), new Project(), new Project());
        existingPrivate.forEach(p -> p.setVisibility(ProjectVisibility.PRIVATE));

        when(projectRepository.findByOwnerId(ownerId)).thenReturn(existingPrivate);

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(BadRequestException.class);
    }

    // --- getProjectById Tests ---

    @Test
    void getProjectByIdShouldReturnProject() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder()
                .projectId(projectId)
                .visibility(ProjectVisibility.PUBLIC)
                .build();

        when(projectRepository.findByProjectId(projectId)).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.getProjectById(projectId, UUID.randomUUID(), "ROLE_USER");

        assertThat(response.getProjectId()).isEqualTo(projectId);
    }

    @Test
    void getProjectByIdShouldAllowMemberOfPrivate() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Project project = Project.builder().projectId(projectId).ownerId(UUID.randomUUID()).visibility(ProjectVisibility.PRIVATE).build();
        when(projectRepository.findByProjectId(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectProjectIdAndUserId(projectId, userId)).thenReturn(true);
        ProjectResponse response = projectService.getProjectById(projectId, userId, "ROLE_USER");
        assertThat(response.getProjectId()).isEqualTo(projectId);
    }

    @Test
    void getProjectByIdShouldThrowForbiddenForAdminIfRoleNotExact() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().projectId(projectId).ownerId(UUID.randomUUID()).visibility(ProjectVisibility.PRIVATE).build();
        when(projectRepository.findByProjectId(projectId)).thenReturn(Optional.of(project));
        assertThatThrownBy(() -> projectService.getProjectById(projectId, UUID.randomUUID(), "ROLE_MODERATOR"))
                .isInstanceOf(ForbiddenException.class);
    }

    // --- Update/Archive/Delete Tests ---

    @Test
    void updateProjectShouldUpdateFields() {
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Project project = Project.builder().projectId(projectId).ownerId(ownerId).build();
        UpdateProjectRequest request = new UpdateProjectRequest("New Name", "New Desc", "Python", ProjectVisibility.PUBLIC);

        when(projectRepository.findByProjectId(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProjectResponse response = projectService.updateProject(projectId, request, ownerId, "ROLE_USER");
        assertThat(response.getName()).isEqualTo("New Name");
    }

    @Test
    void updateProjectShouldOnlyUpdateNonNullFields() {
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Project project = Project.builder().projectId(projectId).ownerId(ownerId).name("Old").language("Java").build();
        UpdateProjectRequest request = new UpdateProjectRequest(null, null, null, null);
        when(projectRepository.findByProjectId(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        ProjectResponse response = projectService.updateProject(projectId, request, ownerId, "ROLE_USER");
        assertThat(response.getName()).isEqualTo("Old");
    }

    @Test
    void archiveProjectShouldSetArchived() {
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Project project = Project.builder().projectId(projectId).ownerId(ownerId).isArchived(false).build();
        when(projectRepository.findByProjectId(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        ProjectResponse response = projectService.archiveProject(projectId, ownerId, "ROLE_USER");
        assertThat(response.isArchived()).isTrue();
    }

    @Test
    void deleteProjectShouldCallRepository() {
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Project project = Project.builder().projectId(projectId).ownerId(ownerId).build();
        when(projectRepository.findByProjectId(projectId)).thenReturn(Optional.of(project));
        projectService.deleteProject(projectId, ownerId, "ROLE_USER");
        verify(projectRepository).delete(project);
    }

    // --- Fork/Star Tests ---

    @Test
    void forkProjectShouldUseSourceFieldsIfRequestFieldsNull() {
        UUID projectId = UUID.randomUUID();
        Project source = Project.builder().projectId(projectId).name("Source").description("Desc").language("Java").build();
        ForkProjectRequest request = ForkProjectRequest.builder().userId(UUID.randomUUID()).build();
        when(projectRepository.findByProjectId(projectId)).thenReturn(Optional.of(source));
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        ProjectResponse response = projectService.forkProject(projectId, request);
        assertThat(response.getName()).isEqualTo("Source-fork");
    }

    @Test
    void starProjectShouldIncrementCount() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Project project = Project.builder().projectId(projectId).starCount(5).build();
        when(projectRepository.findByProjectId(projectId)).thenReturn(Optional.of(project));
        when(projectStarRepository.existsByProjectProjectIdAndUserId(projectId, userId)).thenReturn(false);
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        ProjectResponse response = projectService.starProject(projectId, userId);
        assertThat(response.getStarCount()).isEqualTo(6);
    }

    // --- Member Tests ---

    @Test
    void addMemberShouldAllowAdmin() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().projectId(projectId).ownerId(UUID.randomUUID()).visibility(ProjectVisibility.PRIVATE).build();
        when(projectRepository.findByProjectId(projectId)).thenReturn(Optional.of(project));
        projectService.addMember(projectId, UUID.randomUUID(), ProjectMemberRole.COLLABORATOR, UUID.randomUUID(), "ROLE_ADMIN");
        verify(projectMemberRepository).save(any());
    }

    @Test
    void addMemberShouldNotSaveIfAlreadyMember() {
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Project project = Project.builder().projectId(projectId).ownerId(ownerId).build();
        when(projectRepository.findByProjectId(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectProjectIdAndUserId(projectId, ownerId)).thenReturn(true);
        projectService.addMember(projectId, ownerId, ProjectMemberRole.OWNER, ownerId, "ROLE_USER");
        verify(projectMemberRepository, never()).save(any());
    }

    // --- List/Search Tests ---

    @Test
    void getAllProjectsShouldReturnList() {
        when(projectRepository.findAll()).thenReturn(Collections.singletonList(new Project()));
        assertThat(projectService.getAllProjects()).hasSize(1);
    }

    @Test
    void getProjectsByOwnerShouldReturnList() {
        UUID ownerId = UUID.randomUUID();
        when(projectRepository.findByOwnerId(ownerId)).thenReturn(Collections.singletonList(new Project()));
        assertThat(projectService.getProjectsByOwner(ownerId)).hasSize(1);
    }

    @Test
    void getPublicProjectsShouldReturnList() {
        when(projectRepository.findByVisibilityAndIsArchived(ProjectVisibility.PUBLIC, false)).thenReturn(Collections.singletonList(new Project()));
        assertThat(projectService.getPublicProjects()).hasSize(1);
    }

    @Test
    void searchProjectsShouldReturnList() {
        when(projectRepository.searchByName("test")).thenReturn(Collections.singletonList(new Project()));
        assertThat(projectService.searchProjects("test")).hasSize(1);
    }

    @Test
    void searchProjectsShouldReturnEmptyForBlank() {
        assertThat(projectService.searchProjects("")).isEmpty();
        assertThat(projectService.searchProjects(null)).isEmpty();
    }

    @Test
    void getProjectsByLanguageShouldReturnList() {
        when(projectRepository.findByLanguageIgnoreCaseAndVisibilityAndIsArchived(anyString(), any(), anyBoolean())).thenReturn(Collections.singletonList(new Project()));
        assertThat(projectService.getProjectsByLanguage("Java")).hasSize(1);
    }

    @Test
    void getProjectsByLanguageShouldReturnEmptyForBlank() {
        assertThat(projectService.getProjectsByLanguage("")).isEmpty();
        assertThat(projectService.getProjectsByLanguage(null)).isEmpty();
    }

    @Test
    void getProjectsByMemberShouldReturnList() {
        UUID userId = UUID.randomUUID();
        when(projectRepository.findByMemberUserId(userId)).thenReturn(Collections.singletonList(new Project()));
        assertThat(projectService.getProjectsByMember(userId)).hasSize(1);
    }

    @Test
    void getProjectMembersShouldReturnList() {
        UUID projectId = UUID.randomUUID();
        when(projectMemberRepository.findByProjectProjectId(projectId)).thenReturn(Collections.singletonList(new ProjectMember()));
        assertThat(projectService.getProjectMembers(projectId)).hasSize(1);
    }
}
