package com.codesync.file.repository;

import com.codesync.file.entity.CodeFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<CodeFile, UUID> {

    Optional<CodeFile> findByFileId(UUID fileId);

    List<CodeFile> findByProjectIdAndIsDeleted(UUID projectId, boolean isDeleted);

    List<CodeFile> findByProjectIdAndFolderIdAndIsDeleted(UUID projectId, UUID folderId, boolean isDeleted);

    List<CodeFile> findByProjectIdAndFolderIdIsNullAndIsDeleted(UUID projectId, boolean isDeleted);

    @Query("SELECT f FROM CodeFile f WHERE f.projectId = :projectId AND f.isDeleted = false " +
           "AND (LOWER(CAST(f.name AS string)) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(CAST(f.content AS string)) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<CodeFile> searchInProject(@Param("projectId") UUID projectId, @Param("query") String query);
}
