package com.codesync.comment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID commentId;

    private String projectId;
    private String fileId;
    private String authorId;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer lineNumber;
    private Integer columnNumber;

    private UUID parentCommentId;

    @Builder.Default
    private Boolean resolved = false;

    private UUID snapshotId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
