package com.codesync.version.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Snapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID snapshotId;

    private String projectId;

    private String fileId;

    private String authorId;

    private String message;

    @Lob
    @Column(columnDefinition="TEXT")
    private String content;

    private String hash; // SHA-256

    private UUID parentSnapshotId;

    private String branch;

    private String tag;

    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
