package com.codesync.file.repository;

import com.codesync.file.entity.CodeFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FolderRepository extends JpaRepository<CodeFolder, UUID> {
    List<CodeFolder> findByProjectIdAndIsDeleted(UUID projectId, boolean isDeleted);
    Optional<CodeFolder> findByFolderId(UUID folderId);
    
    boolean existsByNameAndProjectIdAndParentFolderIdAndIsDeleted(String name, UUID projectId, UUID parentFolderId, boolean isDeleted);
    
    boolean existsByNameAndProjectIdAndParentFolderIdIsNullAndIsDeleted(String name, UUID projectId, boolean isDeleted);
}
