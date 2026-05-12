package com.codesync.project.dto.request;

import com.codesync.project.entity.ProjectVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    @NotNull(message = "Owner id is required")
    private UUID ownerId;

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    @NotBlank(message = "Language is required")
    private String language;

    private ProjectVisibility visibility;
    private Set<UUID> memberUserIds;

    private boolean isPremium;
}
