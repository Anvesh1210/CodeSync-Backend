package com.codesync.website.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {
    private String sessionId;
    private String projectId;
    private String fileId;
    
    @JsonProperty("hostId")
    @JsonAlias("ownerId")
    private String ownerId;
    
    private String projectOwnerId;
    
    private String status;
    private Integer maxParticipants;
    private boolean isPasswordProtected;
    private String createdAt;
    private String endedAt;
}
