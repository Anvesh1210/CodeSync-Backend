package com.codesync.project.repository;

import com.codesync.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    List<ProjectMember> findByProjectProjectId(UUID projectId);

    List<ProjectMember> findByUserId(UUID userId);

    boolean existsByProjectProjectIdAndUserId(UUID projectId, UUID userId);

    Optional<ProjectMember> findByProjectProjectIdAndUserId(UUID projectId, UUID userId);

    void deleteByProjectProjectIdAndUserId(UUID projectId, UUID userId);
}
