package com.codesync.project.dto.response;

import com.codesync.project.entity.ProjectVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private UUID projectId;
    private UUID ownerId;
    private String name;
    private String description;
    private String language;
    private ProjectVisibility visibility;
    private boolean isArchived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int starCount;
    private int forkCount;
    private UUID sourceProjectId;
    private java.util.Set<UUID> memberUserIds;
}
