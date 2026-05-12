package com.codesync.project.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.codesync.project.dto.request.CreateProjectRequest;
import com.codesync.project.dto.request.ForkProjectRequest;
import com.codesync.project.dto.request.StarProjectRequest;
import com.codesync.project.dto.request.UpdateProjectRequest;
import com.codesync.project.dto.response.ProjectResponse;
import com.codesync.project.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ProjectController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createProjectShouldReturn201() throws Exception {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .ownerId(UUID.randomUUID())
                .name("Test Project")
                .language("Java")
                .build();
        ProjectResponse response = ProjectResponse.builder().projectId(UUID.randomUUID()).build();

        when(projectService.createProject(any())).thenReturn(response);

        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").exists());
    }

    @Test
    void getProjectByIdShouldReturn200() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectService.getProjectById(eq(projectId), any(), anyString())).thenReturn(new ProjectResponse());

        mockMvc.perform(get("/projects/{projectId}", projectId))
                .andExpect(status().isOk());
    }

    @Test
    void getAllProjectsShouldReturnList() throws Exception {
        when(projectService.getAllProjects()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk());
    }

    @Test
    void getProjectsByOwnerShouldReturnList() throws Exception {
        UUID ownerId = UUID.randomUUID();
        when(projectService.getProjectsByOwner(ownerId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/projects/owner/{ownerId}", ownerId))
                .andExpect(status().isOk());
    }

    @Test
    void getPublicProjectsShouldReturnList() throws Exception {
        when(projectService.getPublicProjects()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/projects/public"))
                .andExpect(status().isOk());
    }

    @Test
    void searchProjectsShouldReturnList() throws Exception {
        when(projectService.searchProjects("test")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/projects/search").param("query", "test"))
                .andExpect(status().isOk());
    }

    @Test
    void getProjectsByMemberShouldReturnList() throws Exception {
        UUID userId = UUID.randomUUID();
        when(projectService.getProjectsByMember(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/projects/member/{userId}", userId))
                .andExpect(status().isOk());
    }

    @Test
    void getProjectsByLanguageShouldReturnList() throws Exception {
        when(projectService.getProjectsByLanguage("Java")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/projects/language/Java"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProjectShouldReturn200() throws Exception {
        UUID projectId = UUID.randomUUID();
        UpdateProjectRequest request = new UpdateProjectRequest("New Name", null, null, null);
        when(projectService.updateProject(eq(projectId), any(), any(), anyString())).thenReturn(new ProjectResponse());

        mockMvc.perform(put("/projects/{projectId}", projectId)
                .header("X-Caller-Id", UUID.randomUUID().toString())
                .header("X-Caller-Role", "ROLE_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void archiveProjectShouldReturn200() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectService.archiveProject(eq(projectId), any(), anyString())).thenReturn(new ProjectResponse());

        mockMvc.perform(put("/projects/{projectId}/archive", projectId)
                .header("X-Caller-Id", UUID.randomUUID().toString())
                .header("X-Caller-Role", "ROLE_USER"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProjectShouldReturn200() throws Exception {
        UUID projectId = UUID.randomUUID();
        doNothing().when(projectService).deleteProject(eq(projectId), any(), anyString());

        mockMvc.perform(delete("/projects/{projectId}", projectId)
                .header("X-Caller-Id", UUID.randomUUID().toString())
                .header("X-Caller-Role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Project deleted successfully"));
    }

    @Test
    void forkProjectShouldReturn201() throws Exception {
        UUID projectId = UUID.randomUUID();
        ForkProjectRequest request = ForkProjectRequest.builder().userId(UUID.randomUUID()).build();
        when(projectService.forkProject(eq(projectId), any())).thenReturn(new ProjectResponse());

        mockMvc.perform(post("/projects/{projectId}/fork", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void starProjectShouldReturn200() throws Exception {
        UUID projectId = UUID.randomUUID();
        StarProjectRequest request = new StarProjectRequest(UUID.randomUUID());
        when(projectService.starProject(eq(projectId), any())).thenReturn(new ProjectResponse());

        mockMvc.perform(put("/projects/{projectId}/star", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void addMemberShouldReturn200() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doNothing().when(projectService).addMember(eq(projectId), eq(userId), any(), any(), anyString());

        mockMvc.perform(post("/projects/{projectId}/members/{userId}", projectId, userId)
                .header("X-Caller-Id", UUID.randomUUID().toString())
                .header("X-Caller-Role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Member added successfully"));
    }

    @Test
    void getProjectMembersShouldReturnList() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectService.getProjectMembers(projectId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/projects/{projectId}/members", projectId))
                .andExpect(status().isOk());
    }
}
