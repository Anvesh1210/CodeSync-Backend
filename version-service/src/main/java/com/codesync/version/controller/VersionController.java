package com.codesync.version.controller;

import com.codesync.version.dto.DiffResult;
import com.codesync.version.dto.SnapshotRequest;
import com.codesync.version.entity.Snapshot;
import com.codesync.version.service.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/versions")
public class VersionController {

    @Autowired
    private VersionService versionService;

    @PostMapping
    public ResponseEntity<Snapshot> createSnapshot(@RequestBody SnapshotRequest request) {
        return ResponseEntity.ok(versionService.createSnapshot(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Snapshot> getSnapshotById(@PathVariable UUID id) {
        return ResponseEntity.ok(versionService.getSnapshotById(id));
    }

    @GetMapping("/file/{fileId}")
    public ResponseEntity<List<Snapshot>> getSnapshotsByFile(@PathVariable String fileId) {
        return ResponseEntity.ok(versionService.getSnapshotsByFile(fileId));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Snapshot>> getSnapshotsByProject(@PathVariable String projectId) {
        return ResponseEntity.ok(versionService.getSnapshotsByProject(projectId));
    }

    @GetMapping("/branch/{branch}")
    public ResponseEntity<List<Snapshot>> getSnapshotsByBranch(@PathVariable String branch) {
        return ResponseEntity.ok(versionService.getSnapshotsByBranch(branch));
    }

    @GetMapping("/history/{fileId}")
    public ResponseEntity<List<Snapshot>> getFileHistory(@PathVariable String fileId) {
        return ResponseEntity.ok(versionService.getFileHistory(fileId));
    }

    @GetMapping("/latest/{fileId}")
    public ResponseEntity<Snapshot> getLatestSnapshot(@PathVariable String fileId) {
        return ResponseEntity.ok(versionService.getLatestSnapshot(fileId));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Snapshot> restoreSnapshot(@PathVariable UUID id, @RequestParam String authorId) {
        return ResponseEntity.ok(versionService.restoreSnapshot(id, authorId));
    }

    @GetMapping("/diff")
    public ResponseEntity<DiffResult> diffSnapshots(@RequestParam UUID oldId, @RequestParam UUID newId) {
        return ResponseEntity.ok(versionService.diffSnapshots(oldId, newId));
    }

    @PostMapping("/{id}/branch")
    public ResponseEntity<Snapshot> createBranch(@PathVariable UUID id, @RequestParam String newBranch) {
        return ResponseEntity.ok(versionService.createBranch(id, newBranch));
    }

    @PostMapping("/{id}/tag")
    public ResponseEntity<Snapshot> tagSnapshot(@PathVariable UUID id, @RequestParam String tag) {
        return ResponseEntity.ok(versionService.tagSnapshot(id, tag));
    }
}
