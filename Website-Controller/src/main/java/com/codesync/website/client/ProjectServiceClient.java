package com.codesync.website.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "project-service")
public interface ProjectServiceClient {

    @GetMapping("/projects")
    Object getAllProjects();

    @GetMapping("/projects/public")
    Object getPublicProjects();

    @GetMapping("/projects/{projectId}")
    Object getProjectById(@PathVariable("projectId") String projectId, @RequestParam(value = "userId", required = false) String userId, @RequestHeader("X-Caller-Role") String callerRole);

    @GetMapping("/projects/owner/{ownerId}")
    Object getProjectsByOwner(@PathVariable("ownerId") String ownerId);

    @GetMapping("/projects/member/{memberUserId}")
    Object getProjectsByMember(@PathVariable("memberUserId") String memberUserId);

    @GetMapping("/projects/search")
    Object searchProjects(@RequestParam("query") String query);

    @GetMapping("/projects/language/{language}")
    Object getProjectsByLanguage(@PathVariable("language") String language);

    @PostMapping("/projects")
    Object createProject(@RequestBody Object request);

    @PutMapping("/projects/{projectId}")
    Object updateProject(@PathVariable("projectId") String projectId, @RequestBody Object request, @RequestHeader("X-Caller-Id") String callerId, @RequestHeader("X-Caller-Role") String callerRole);

    @PutMapping("/projects/{projectId}/archive")
    Object archiveProject(@PathVariable("projectId") String projectId, @RequestHeader("X-Caller-Id") String callerId, @RequestHeader("X-Caller-Role") String callerRole);

    @DeleteMapping("/projects/{projectId}")
    Object deleteProject(@PathVariable("projectId") String projectId, @RequestHeader("X-Caller-Id") String callerId, @RequestHeader("X-Caller-Role") String callerRole);

    @PostMapping("/projects/{projectId}/fork")
    Object forkProject(@PathVariable("projectId") String projectId, @RequestBody Object request);

    @PutMapping("/projects/{projectId}/star")
    Object starProject(@PathVariable("projectId") String projectId, @RequestBody Object request);

    @PostMapping("/projects/{projectId}/members/{userId}")
    Object addMember(@PathVariable("projectId") String projectId, @PathVariable("userId") String userId, @RequestParam("role") String role, @RequestHeader("X-Caller-Id") String callerId, @RequestHeader("X-Caller-Role") String callerRole);

    @GetMapping("/projects/{projectId}/members")
    java.util.List<java.util.Map<String, Object>> getProjectMembers(@PathVariable("projectId") String projectId);
}
