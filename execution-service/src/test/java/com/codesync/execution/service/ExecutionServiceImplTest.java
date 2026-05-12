package com.codesync.execution.service;

import com.codesync.execution.dto.*;
import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.JobStatus;
import com.codesync.execution.repository.ExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceImplTest {

    @Mock
    private ExecutionRepository executionRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private DockerService dockerService;

    @InjectMocks
    private ExecutionServiceImpl executionService;

    private UUID userId;
    private UUID projectId;
    private UUID jobId;
    private ExecutionJob mockJob;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        jobId = UUID.randomUUID();

        mockJob = ExecutionJob.builder()
                .jobId(jobId)
                .userId(userId)
                .projectId(projectId)
                .language("java")
                .sourceCode("public class Main {}")
                .status(JobStatus.QUEUED)
                .build();
    }

    @Test
    void submitExecution_ShouldSucceed_WhenLanguageSupported() {
        ExecutionRequest request = new ExecutionRequest();
        request.setUserId(userId);
        request.setProjectId(projectId);
        request.setLanguage("java");
        request.setSourceCode("public class Main {}");

        when(executionRepository.save(any(ExecutionJob.class))).thenReturn(mockJob);

        ExecutionResponse response = executionService.submitExecution(request);

        assertNotNull(response);
        assertEquals(jobId, response.getJobId());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), eq(jobId));
    }

    @Test
    void submitExecution_ShouldThrowException_WhenLanguageNotSupported() {
        ExecutionRequest request = new ExecutionRequest();
        request.setLanguage("unknown");

        assertThrows(RuntimeException.class, () -> executionService.submitExecution(request));
    }

    @Test
    void lintCode_ShouldSucceed() {
        ExecutionRequest request = new ExecutionRequest();
        request.setLanguage("java");
        request.setUserId(userId);

        when(dockerService.lintCode(request)).thenReturn(Collections.emptyList());

        List<SyntaxErrorDTO> errors = executionService.lintCode(request);

        assertNotNull(errors);
        verify(dockerService).lintCode(request);
    }

    @Test
    void getJobById_ShouldReturnJob() {
        when(executionRepository.findByJobId(jobId)).thenReturn(Optional.of(mockJob));

        ExecutionResponse response = executionService.getJobById(jobId);

        assertEquals(jobId, response.getJobId());
    }

    @Test
    void getExecutionsByUser_ShouldReturnList() {
        when(executionRepository.findByUserId(userId)).thenReturn(List.of(mockJob));

        List<ExecutionResponse> results = executionService.getExecutionsByUser(userId);

        assertEquals(1, results.size());
    }

    @Test
    void cancelExecution_ShouldWork_WhenQueued() {
        when(executionRepository.findByJobId(jobId)).thenReturn(Optional.of(mockJob));
        when(executionRepository.save(any(ExecutionJob.class))).thenReturn(mockJob);

        ExecutionResponse response = executionService.cancelExecution(jobId);

        assertEquals(JobStatus.CANCELLED, response.getStatus());
    }

    @Test
    void getExecutionResult_ShouldThrow_WhenNotCompleted() {
        when(executionRepository.findByJobId(jobId)).thenReturn(Optional.of(mockJob));

        assertThrows(RuntimeException.class, () -> executionService.getExecutionResult(jobId));
    }

    @Test
    void getSupportedLanguages_ShouldReturnList() {
        List<LanguageInfo> langs = executionService.getSupportedLanguages();
        assertFalse(langs.isEmpty());
    }

    @Test
    void getLanguageVersion_ShouldReturnInfo() {
        LanguageInfo info = executionService.getLanguageVersion("java");
        assertEquals("java", info.getName());
    }

    @Test
    void getExecutionStats_ShouldReturnStats() {
        when(executionRepository.findAll()).thenReturn(List.of(mockJob));
        when(executionRepository.count()).thenReturn(1L);
        when(executionRepository.countByStatus(JobStatus.QUEUED)).thenReturn(1L);

        ExecutionStats stats = executionService.getExecutionStats();

        assertNotNull(stats);
        assertEquals(1, stats.getTotalExecutions());
    }

    @Test
    void getAllExecutions_ShouldReturnList() {
        when(executionRepository.findAll()).thenReturn(List.of(mockJob));

        List<ExecutionResponse> results = executionService.getAllExecutions();

        assertEquals(1, results.size());
    }
}
