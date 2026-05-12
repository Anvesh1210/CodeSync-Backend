package com.codesync.execution.controller;

import com.codesync.execution.dto.*;
import com.codesync.execution.service.ExecutionService;
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
@RequestMapping("/executions")
@RequiredArgsConstructor
@Tag(name = "Execution Resource", description = "Endpoints for managing code execution jobs")
public class ExecutionController {

    private final ExecutionService executionService;

    @PostMapping
    @Operation(summary = "Submit a code execution job")
    public ResponseEntity<ExecutionResponse> submitExecution(@Valid @RequestBody ExecutionRequest request) {
        return new ResponseEntity<>(executionService.submitExecution(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all execution jobs (Admin)")
    public ResponseEntity<List<ExecutionResponse>> getAllExecutions() {
        return ResponseEntity.ok(executionService.getAllExecutions());
    }

    @PostMapping("/lint")
    @Operation(summary = "Perform a syntax check without full execution")
    public ResponseEntity<List<SyntaxErrorDTO>> lintCode(@Valid @RequestBody ExecutionRequest request) {
        return ResponseEntity.ok(executionService.lintCode(request));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get execution job by ID")
    public ResponseEntity<ExecutionResponse> getJobById(@PathVariable UUID jobId) {
        return ResponseEntity.ok(executionService.getJobById(jobId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get execution jobs by user ID")
    public ResponseEntity<List<ExecutionResponse>> getExecutionsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(executionService.getExecutionsByUser(userId));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get execution jobs by project ID")
    public ResponseEntity<List<ExecutionResponse>> getExecutionsByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(executionService.getExecutionsByProject(projectId));
    }

    @PostMapping("/{jobId}/cancel")
    @Operation(summary = "Cancel a queued or running execution job")
    public ResponseEntity<ExecutionResponse> cancelExecution(@PathVariable UUID jobId) {
        return ResponseEntity.ok(executionService.cancelExecution(jobId));
    }

    @GetMapping("/{jobId}/result")
    @Operation(summary = "Get the result of an execution job")
    public ResponseEntity<ExecutionResponse> getExecutionResult(@PathVariable UUID jobId) {
        return ResponseEntity.ok(executionService.getExecutionResult(jobId));
    }

    @GetMapping("/languages")
    @Operation(summary = "Get list of supported programming languages")
    public ResponseEntity<List<LanguageInfo>> getSupportedLanguages() {
        return ResponseEntity.ok(executionService.getSupportedLanguages());
    }

    @GetMapping("/languages/{language}")
    @Operation(summary = "Get version info for a specific language")
    public ResponseEntity<LanguageInfo> getLanguageVersion(@PathVariable String language) {
        return ResponseEntity.ok(executionService.getLanguageVersion(language));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform execution statistics")
    public ResponseEntity<ExecutionStats> getExecutionStats() {
        return ResponseEntity.ok(executionService.getExecutionStats());
    }
}
