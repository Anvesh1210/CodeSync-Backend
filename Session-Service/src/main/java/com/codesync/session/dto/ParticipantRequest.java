package com.codesync.session.dto;

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
public class ParticipantRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    private Integer cursorLine;

    private Integer cursorCol;

    private String color;

    private String password;
    private String role;
}
