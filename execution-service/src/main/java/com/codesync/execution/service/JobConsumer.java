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
        log.info(">>>> [JobConsumer] Received message for job {} from RabbitMQ", jobId);
        
        ExecutionJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error(">>>> [JobConsumer] CRITICAL: Job {} not found in database! It might have been deleted or transaction failed.", jobId);
            return;
        }
        
        log.info(">>>> [JobConsumer] Starting processing for job {} | Project: {} | Language: {}", 
                job.getJobId(), job.getProjectId(), job.getLanguage());

        try {
            log.info(">>>> [JobConsumer] Updating status to RUNNING for job {}", jobId);
            job.setStatus(com.codesync.execution.entity.JobStatus.RUNNING);
            jobRepository.save(job);
            
            // Broadcast status update
            String statusTopic = "/topic/execution/" + jobId + "/status";
            log.info("[PUBLISH] Publishing status RUNNING to topic: {}", statusTopic);
            messagingTemplate.convertAndSend(statusTopic, "RUNNING");
            
            log.info(">>>> [JobConsumer] Invoking DockerService for job {}", jobId);
            String outputTopic = "/topic/execution/" + jobId + "/output";
            dockerService.executeJob(job, job.isPremium(), output -> {
                // Stream output to WebSocket - synchronized to preserve order
                synchronized (this) {
                    log.info("[PUBLISH] Publishing output to topic: {}", outputTopic);
                    messagingTemplate.convertAndSend(outputTopic, output);
                }
            });
            
            if (job.getStatus() != com.codesync.execution.entity.JobStatus.TIME_OUT) {
                log.info(">>>> [JobConsumer] Job {} execution finished. Setting status to COMPLETED", jobId);
                job.setStatus(com.codesync.execution.entity.JobStatus.COMPLETED);
            }
        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage());
            job.setStatus(com.codesync.execution.entity.JobStatus.FAILED);
            job.setStderr(e.getMessage());
            String outputTopic = "/topic/execution/" + jobId + "/output";
            log.info("[PUBLISH] Publishing error output to topic: {}", outputTopic);
            messagingTemplate.convertAndSend(outputTopic, "ERR: " + e.getMessage());
        } finally {
            jobRepository.save(job);
            // Send final status
            String finalStatus = job.getStatus().toString();
            String statusTopic = "/topic/execution/" + jobId + "/status";
            log.info("[PUBLISH] Publishing final status {} to topic: {}", finalStatus, statusTopic);
            messagingTemplate.convertAndSend(statusTopic, finalStatus);
            
            // Send result back to result queue
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_EXECUTION, RabbitMQConfig.ROUTING_KEY_RESULTS, jobId);
            log.info("Job {} completed and result queued", jobId);
        }
    }
}
