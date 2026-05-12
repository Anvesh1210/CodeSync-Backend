package com.codesync.website.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantRequestDto {
    private String userId;
    private Integer cursorLine;
    private Integer cursorCol;
    private String color;
    private String password;
    private String role;
}
