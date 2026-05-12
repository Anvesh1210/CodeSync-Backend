package com.codesync.execution.service;

import com.codesync.execution.config.RabbitMQConfig;
import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.repository.ExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobConsumer {

    private final DockerService dockerService;
    private final ExecutionRepository jobRepository;
    private final RabbitTemplate rabbitTemplate;

    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_JOBS)
    public void consumeJob(UUID jobId) {
        log.info("Received job {} from queue", jobId);
        
        ExecutionJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Job {} not found in repository", jobId);
            return;
        }

        try {
            job.setStatus(com.codesync.execution.entity.JobStatus.RUNNING);
            jobRepository.save(job);
            
            // Broadcast status update
            messagingTemplate.convertAndSend("/topic/execution/" + jobId + "/status", "RUNNING");
            
            dockerService.executeJob(job, job.isPremium(), output -> {
                // Stream output to WebSocket
                messagingTemplate.convertAndSend("/topic/execution/" + jobId + "/output", output);
            });
            
            if (job.getStatus() != com.codesync.execution.entity.JobStatus.TIME_OUT) {
                job.setStatus(com.codesync.execution.entity.JobStatus.COMPLETED);
            }
        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage());
            job.setStatus(com.codesync.execution.entity.JobStatus.FAILED);
            job.setStderr(e.getMessage());
            messagingTemplate.convertAndSend("/topic/execution/" + jobId + "/output", "ERR: " + e.getMessage());
        } finally {
            jobRepository.save(job);
            // Send final status
            messagingTemplate.convertAndSend("/topic/execution/" + jobId + "/status", job.getStatus().toString());
            // Send result back to result queue
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_EXECUTION, RabbitMQConfig.ROUTING_KEY_RESULTS, jobId);
            log.info("Job {} completed and result queued", jobId);
        }
    }
}
