package com.codesync.session.repository;

import com.codesync.session.entity.CollabSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CollabRepository extends JpaRepository<CollabSession, UUID> {

    List<CollabSession> findByProjectId(UUID projectId);

    List<CollabSession> findByProjectIdAndStatus(UUID projectId, CollabSession.SessionStatus status);

    List<CollabSession> findByStatus(CollabSession.SessionStatus status);
}
