package com.codesync.execution.service;

import com.codesync.execution.dto.*;
import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.JobStatus;
import com.codesync.execution.repository.ExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.amqp.core.AmqpTemplate;
import com.codesync.execution.config.RabbitMQConfig;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionServiceImpl implements ExecutionService {

    private final ExecutionRepository executionRepository;
    private final AmqpTemplate amqpTemplate;
    private final DockerService dockerService;

    private final LanguageRegistry languageRegistry;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ExecutionResponse submitExecution(ExecutionRequest request) {
        log.info("Submitting execution for user {} | language={}", request.getUserId(), request.getLanguage());

        if (!languageRegistry.isSupported(request.getLanguage())) {
            log.error("Unsupported language: {}. Supported: {}", request.getLanguage(), languageRegistry.getAllConfigs().stream().map(c -> c.getName()).toList());
            throw new RuntimeException("Unsupported language: " + request.getLanguage());
        }

        ExecutionJob job = ExecutionJob.builder()
                .projectId(request.getProjectId())
                .fileId(request.getFileId())
                .userId(request.getUserId())
                .language(request.getLanguage().toLowerCase())
                .sourceCode(request.getSourceCode())
                .stdin(request.getStdin())
                .fileName(request.getFileName())
                .isPremium(Boolean.TRUE.equals(request.getIsPremium()))
                .status(JobStatus.QUEUED)
                .build();

        ExecutionJob saved = executionRepository.save(job);
        UUID jobId = saved.getJobId();
        log.info("Job {} saved to database with status {}", jobId, saved.getStatus());

        // Publish QUEUED status via WebSocket
        String statusTopic = "/topic/execution/" + jobId + "/status";
        log.info("[PUBLISH] Publishing status QUEUED to topic: {}", statusTopic);
        messagingTemplate.convertAndSend(statusTopic, "QUEUED");

        // Submit to RabbitMQ for asynchronous execution ONLY after transaction commits
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            log.debug("Transaction synchronization active. Registering afterCommit for job {}", jobId);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info(">>>> [ExecutionService] Transaction committed. Sending job {} to RabbitMQ", jobId);
                    try {
                        amqpTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_EXECUTION, 
                                                    RabbitMQConfig.ROUTING_KEY_JOBS, 
                                                    jobId);
                        log.info(">>>> [ExecutionService] Job {} successfully sent to RabbitMQ", jobId);
                    } catch (Exception e) {
                        log.error(">>>> [ExecutionService] Failed to send job {} to RabbitMQ: {}", jobId, e.getMessage());
                    }
                }
            });
        } else {
            log.warn(">>>> [ExecutionService] No active transaction synchronization. Sending job {} to RabbitMQ immediately.", jobId);
            amqpTemplate.convertAndSend(com.codesync.execution.config.RabbitMQConfig.EXCHANGE_EXECUTION, 
                                        com.codesync.execution.config.RabbitMQConfig.ROUTING_KEY_JOBS, 
                                        jobId);
        }

        return toResponse(saved);
    }

    @Override
    public List<SyntaxErrorDTO> lintCode(ExecutionRequest request) {
        log.info("Linting code for user {} | language={}", request.getUserId(), request.getLanguage());
        if (!languageRegistry.isSupported(request.getLanguage())) {
            throw new RuntimeException("Unsupported language: " + request.getLanguage());
        }
        return dockerService.lintCode(request);
    }

    @Override
    public ExecutionResponse getJobById(UUID jobId) {
        return toResponse(findJob(jobId));
    }

    @Override
    public List<ExecutionResponse> getExecutionsByUser(UUID userId) {
        log.info("Fetching executions for user {}", userId);
        return executionRepository.findByUserId(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ExecutionResponse> getExecutionsByProject(UUID projectId) {
        log.info("Fetching executions for project {}", projectId);
        return executionRepository.findByProjectId(projectId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExecutionResponse cancelExecution(UUID jobId) {
        log.info("Cancelling job {}", jobId);
        ExecutionJob job = findJob(jobId);
        if (job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.RUNNING) {
            job.setStatus(JobStatus.CANCELLED);
            job.setCompletedAt(LocalDateTime.now());
            return toResponse(executionRepository.save(job));
        }
        throw new RuntimeException("Job " + jobId + " cannot be cancelled (status=" + job.getStatus() + ")");
    }

    @Override
    public ExecutionResponse getExecutionResult(UUID jobId) {
        log.info("Fetching result for job {}", jobId);
        ExecutionJob job = findJob(jobId);
        if (job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.RUNNING) {
            throw new RuntimeException("Job " + jobId + " has not completed yet (status=" + job.getStatus() + ")");
        }
        return toResponse(job);
    }

    @Override
    public List<LanguageInfo> getSupportedLanguages() {
        return languageRegistry.getAllConfigs().stream()
                .map(config -> LanguageInfo.builder()
                        .name(config.getName())
                        .displayName(config.getName().substring(0, 1).toUpperCase() + config.getName().substring(1))
                        .version("latest") // In a real app, this might come from the config or container
                        .extension(config.getExtension())
                        .boilerplate(config.getBoilerplate())
                        .defaultFileName(config.getDefaultFileName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public LanguageInfo getLanguageVersion(String language) {
        return languageRegistry.getConfig(language)
                .map(config -> LanguageInfo.builder()
                        .name(config.getName())
                        .displayName(config.getName())
                        .version("latest")
                        .extension(config.getExtension())
                        .boilerplate(config.getBoilerplate())
                        .defaultFileName(config.getDefaultFileName())
                        .build())
                .orElseThrow(() -> new RuntimeException("Unsupported language: " + language));
    }

    @Override
    public ExecutionStats getExecutionStats() {
        log.info("Computing execution stats");

        // Per-language counts
        Map<String, Long> byLanguage = executionRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(ExecutionJob::getLanguage, Collectors.counting()));

        return ExecutionStats.builder()
                .totalExecutions(executionRepository.count())
                .queuedExecutions(executionRepository.countByStatus(JobStatus.QUEUED))
                .runningExecutions(executionRepository.countByStatus(JobStatus.RUNNING))
                .completedExecutions(executionRepository.countByStatus(JobStatus.COMPLETED))
                .failedExecutions(executionRepository.countByStatus(JobStatus.FAILED))
                .cancelledExecutions(executionRepository.countByStatus(JobStatus.CANCELLED))
                .avgExecutionTimeMs(executionRepository.avgExecutionTimeMs())
                .executionsByLanguage(byLanguage)
                .build();
    }

    @Override
    public List<ExecutionResponse> getAllExecutions() {
        log.info("Fetching all executions for admin");
        return executionRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public void refreshLanguages() {
        languageRegistry.refreshCache();
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    /**
     * Status flow: QUEUED → RUNNING → COMPLETED (or FAILED).
     * Called asynchronously by the SandboxPool thread pool.
     */
    // runJob logic moved to JobConsumer

    private ExecutionJob findJob(UUID jobId) {
        return executionRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Execution job not found: " + jobId));
    }

    private ExecutionResponse toResponse(ExecutionJob job) {
        return ExecutionResponse.builder()
                .jobId(job.getJobId())
                .projectId(job.getProjectId())
                .fileId(job.getFileId())
                .userId(job.getUserId())
                .language(job.getLanguage())
                .sourceCode(job.getSourceCode())
                .stdin(job.getStdin())
                .fileName(job.getFileName())
                .status(job.getStatus())
                .stdout(job.getStdout())
                .stderr(job.getStderr())
                .executionTimeMs(job.getExecutionTimeMs())
                .memoryUsedKb(job.getMemoryUsedKb())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
