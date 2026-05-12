package com.codesync.file.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "code_files")
public class CodeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID fileId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String path;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    private String language;

    private UUID createdById;

    private UUID lastEditedBy;

    private boolean isDeleted;

    // For file tree hierarchy: null means root level
    private UUID folderId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
