package com.codesync.project.entity;

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
@Table(name = "project_stars",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"}))
public class ProjectStar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID starId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    private LocalDateTime starredAt;
}
