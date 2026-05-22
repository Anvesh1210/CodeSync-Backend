package com.codesync.version.repository;

import com.codesync.version.entity.Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import java.util.UUID;

@Repository
public interface SnapshotRepository extends JpaRepository<Snapshot, UUID> {

    List<Snapshot> findByProjectId(String projectId);

    List<Snapshot> findByFileIdOrderByCreatedAtDesc(String fileId);

    List<Snapshot> findByAuthorId(String authorId);

    List<Snapshot> findByBranch(String branch);

    Optional<Snapshot> findBySnapshotId(UUID snapshotId);

    Optional<Snapshot> findByHash(String hash);

    List<Snapshot> findByTag(String tag);

    Optional<Snapshot> findFirstByFileIdOrderByCreatedAtDesc(String fileId);
}
