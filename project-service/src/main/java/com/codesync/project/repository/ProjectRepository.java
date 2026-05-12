package com.codesync.project.repository;

import com.codesync.project.entity.Project;
import com.codesync.project.entity.ProjectVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByProjectId(UUID projectId);

    List<Project> findByOwnerId(UUID ownerId);

    List<Project> findByVisibilityAndIsArchived(ProjectVisibility visibility, boolean isArchived);

    List<Project> findByLanguageIgnoreCaseAndVisibilityAndIsArchived(
            String language, ProjectVisibility visibility, boolean isArchived);

    boolean existsByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);

    boolean existsByOwnerIdAndNameIgnoreCaseAndProjectIdNot(UUID ownerId, String name, UUID projectId);

    @Query("SELECT p FROM Project p WHERE p.isArchived = false AND p.visibility = 'PUBLIC' AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Project> searchByName(@Param("name") String name);

    @Query("SELECT DISTINCT p FROM Project p JOIN ProjectMember m ON m.project.projectId = p.projectId WHERE m.userId = :userId AND p.isArchived = false")
    List<Project> findByMemberUserId(@Param("userId") UUID userId);
}
