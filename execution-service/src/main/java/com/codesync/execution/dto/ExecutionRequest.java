package com.codesync.execution.dto;

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
public class ExecutionRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    private UUID fileId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Language is required")
    private String language;

    @NotBlank(message = "Source code is required")
    private String sourceCode;

    private String stdin;

    private boolean isPremium;
}
