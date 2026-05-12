package com.codesync.website.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "version-service")
public interface VersionServiceClient {

    @GetMapping("/api/versions/project/{projectId}/history")
    Object getVersionHistory(@PathVariable("projectId") String projectId);

    @GetMapping("/api/versions/project/{projectId}/branches")
    Object getBranches(@PathVariable("projectId") String projectId);

    @PostMapping("/api/versions/project/{projectId}/branches")
    Object createBranch(@PathVariable("projectId") String projectId, @RequestBody Object branchData);

    @PostMapping("/api/versions/project/{projectId}/snapshots/tag")
    Object tagSnapshot(@PathVariable("projectId") String projectId, @RequestBody Object tagData);

    @GetMapping("/api/versions/project/{projectId}/snapshots/diff")
    Object diffSnapshots(@PathVariable("projectId") String projectId, @RequestParam("base") String base, @RequestParam("compare") String compare);

    @PostMapping("/api/versions/snapshots/{snapshotId}/restore")
    Object restoreSnapshot(@PathVariable("snapshotId") String snapshotId);
}
