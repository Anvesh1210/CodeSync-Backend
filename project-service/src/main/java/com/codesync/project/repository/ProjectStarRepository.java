package com.codesync.project.repository;

import com.codesync.project.entity.ProjectStar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProjectStarRepository extends JpaRepository<ProjectStar, UUID> {

    boolean existsByProjectProjectIdAndUserId(UUID projectId, UUID userId);
}
