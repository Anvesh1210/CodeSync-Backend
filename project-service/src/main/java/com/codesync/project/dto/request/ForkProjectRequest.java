package com.codesync.project.dto.request;

import com.codesync.project.entity.ProjectVisibility;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForkProjectRequest {

    @NotNull(message = "User id is required")
    private UUID userId;

    private String name;
    private String description;
    private ProjectVisibility visibility;
}
