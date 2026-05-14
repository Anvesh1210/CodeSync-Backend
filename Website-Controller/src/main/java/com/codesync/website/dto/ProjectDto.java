package com.codesync.website.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;

@Data
public class ProjectDto {
    private String projectId;
    private String name;
    private String description;
    private String ownerId;
    private String ownerUsername;
    private String language;
    private String visibility;
    private boolean archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int starCount;
    private int forkCount;
    private String sourceProjectId;
    private Set<String> memberUserIds;
    private List<UserDto> members;
}
