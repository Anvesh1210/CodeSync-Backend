package com.codesync.session.repository;

import com.codesync.session.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    List<Participant> findBySessionId(UUID sessionId);

    List<Participant> findBySessionIdAndLeftAtIsNull(UUID sessionId);

    Optional<Participant> findBySessionIdAndUserId(UUID sessionId, UUID userId);

    long countBySessionIdAndLeftAtIsNull(UUID sessionId);
}
