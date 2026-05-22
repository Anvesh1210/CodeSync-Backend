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
@Table(name = "participants")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID participantId;

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private UUID userId;

    private Integer cursorLine;

    private Integer cursorCol;

    @Enumerated(EnumType.STRING)
    private ParticipantRole role;

    private String color;

    @CreationTimestamp
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;
}
