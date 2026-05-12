package com.codesync.file.dto;

import jakarta.validation.constraints.NotBlank;
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
public class FileRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "File name is required")
    private String name;

    // Path defaults to '/' if not provided
    private String path;

    private String language;

    private String content;

    @NotNull(message = "User ID is required")
    private UUID userId;

    // Optional: parent folder ID for tree hierarchy (null = root)
    private UUID parentId;

    private boolean isPremium;
}
