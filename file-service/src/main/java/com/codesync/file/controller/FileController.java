package com.codesync.file.controller;

import com.codesync.file.dto.FileRequest;
import com.codesync.file.dto.FileResponse;
import com.codesync.file.dto.FileTreeNode;
import com.codesync.file.service.FileService;
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
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File and folder management endpoints")
public class FileController {

    private final FileService fileService;

    @PostMapping
    @Operation(summary = "Create a file")
    public ResponseEntity<FileResponse> createFile(@Valid @RequestBody FileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.createFile(request));
    }

    @PostMapping("/folder")
    @Operation(summary = "Create a folder")
    public ResponseEntity<FileResponse> createFolder(@Valid @RequestBody FileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.createFolder(request));
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get file by ID")
    public ResponseEntity<FileResponse> getFileById(@PathVariable UUID fileId) {
        return ResponseEntity.ok(fileService.getFileById(fileId));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get all files in a project")
    public ResponseEntity<List<FileResponse>> getFilesByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(fileService.getFilesByProject(projectId));
    }

    @GetMapping("/{fileId}/content")
    @Operation(summary = "Get file content")
    public ResponseEntity<String> getFileContent(@PathVariable UUID fileId) {
        return ResponseEntity.ok(fileService.getFileContent(fileId));
    }

    @PutMapping("/{fileId}/content")
    @Operation(summary = "Update file content")
    public ResponseEntity<FileResponse> updateFileContent(
            @PathVariable UUID fileId,
            @RequestBody com.codesync.file.dto.FileContentRequest request) {
        return ResponseEntity.ok(fileService.updateFileContent(fileId, request));
    }

    @PutMapping("/{fileId}/rename")
    @Operation(summary = "Rename a file or folder")
    public ResponseEntity<FileResponse> renameFile(
            @PathVariable UUID fileId,
            @RequestParam String newName,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(fileService.renameFile(fileId, newName, userId));
    }

    @PutMapping("/{fileId}/move")
    @Operation(summary = "Move a file to a new path")
    public ResponseEntity<FileResponse> moveFile(
            @PathVariable UUID fileId,
            @RequestParam String newPath,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(fileService.moveFile(fileId, newPath, userId));
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Soft-delete a file")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID fileId,
            @RequestParam UUID userId) {
        fileService.deleteFile(fileId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{fileId}/restore")
    @Operation(summary = "Restore a soft-deleted file")
    public ResponseEntity<FileResponse> restoreFile(@PathVariable UUID fileId) {
        return ResponseEntity.ok(fileService.restoreFile(fileId));
    }

    @GetMapping("/tree/{projectId}")
    @Operation(summary = "Get hierarchical file tree for a project")
    public ResponseEntity<List<FileTreeNode>> getFileTree(@PathVariable UUID projectId) {
        return ResponseEntity.ok(fileService.getFileTree(projectId));
    }

    @GetMapping("/search/{projectId}")
    @Operation(summary = "Search files by name or content in a project")
    public ResponseEntity<List<FileResponse>> searchInProject(
            @PathVariable UUID projectId,
            @RequestParam String query) {
        return ResponseEntity.ok(fileService.searchInProject(projectId, query));
    }
}
