package com.codesync.session.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "collab_sessions")
public class CollabSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID sessionId;

    @Column(nullable = false)
    private UUID projectId;

    private UUID fileId;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private UUID projectOwnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    private Integer maxParticipants;

    private boolean isPasswordProtected;

    private String sessionPassword;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime endedAt;

    public enum SessionStatus {
        ACTIVE, ENDED
    }
}
