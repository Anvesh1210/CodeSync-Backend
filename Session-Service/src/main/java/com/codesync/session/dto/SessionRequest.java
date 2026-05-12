package com.codesync.session.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    private UUID fileId;

    @NotNull(message = "Owner ID is required")
    @JsonAlias("hostId")
    private UUID ownerId;
    
    private UUID projectOwnerId;

    private Integer maxParticipants;

    private boolean isPasswordProtected;

    private String sessionPassword;

    private boolean isPremium;
}
