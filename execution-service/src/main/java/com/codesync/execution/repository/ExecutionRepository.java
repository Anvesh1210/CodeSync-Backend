package com.codesync.execution.repository;

import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExecutionRepository extends JpaRepository<ExecutionJob, UUID> {

    Optional<ExecutionJob> findByJobId(UUID jobId);

    List<ExecutionJob> findByUserId(UUID userId);

    List<ExecutionJob> findByProjectId(UUID projectId);

    List<ExecutionJob> findByStatus(JobStatus status);

    long countByStatus(JobStatus status);

    @Query("SELECT AVG(e.executionTimeMs) FROM ExecutionJob e WHERE e.status = 'COMPLETED'")
    Double avgExecutionTimeMs();
}
