package com.codesync.execution.dto;

import com.codesync.execution.entity.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResponse {

    private UUID jobId;
    private UUID projectId;
    private UUID fileId;
    private UUID userId;
    private String language;
    private String sourceCode;
    private String stdin;
    private JobStatus status;
    private String stdout;
    private String stderr;
    private Long executionTimeMs;
    private Long memoryUsedKb;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
