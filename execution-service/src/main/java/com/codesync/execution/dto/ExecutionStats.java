package com.codesync.execution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionStats {

    private long totalExecutions;
    private long queuedExecutions;
    private long runningExecutions;
    private long completedExecutions;
    private long failedExecutions;
    private long cancelledExecutions;
    private Double avgExecutionTimeMs;
    private Map<String, Long> executionsByLanguage;
}
