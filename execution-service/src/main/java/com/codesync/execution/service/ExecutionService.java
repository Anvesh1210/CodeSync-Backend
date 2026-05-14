package com.codesync.execution.service;

import com.codesync.execution.dto.*;

import java.util.List;
import java.util.UUID;

public interface ExecutionService {

    ExecutionResponse submitExecution(ExecutionRequest request);

    List<SyntaxErrorDTO> lintCode(ExecutionRequest request);

    ExecutionResponse getJobById(UUID jobId);

    List<ExecutionResponse> getExecutionsByUser(UUID userId);

    List<ExecutionResponse> getExecutionsByProject(UUID projectId);

    ExecutionResponse cancelExecution(UUID jobId);

    ExecutionResponse getExecutionResult(UUID jobId);

    List<LanguageInfo> getSupportedLanguages();

    LanguageInfo getLanguageVersion(String language);

    ExecutionStats getExecutionStats();
    List<ExecutionResponse> getAllExecutions();
    void refreshLanguages();
}
