package com.codesync.website.dto;

import lombok.Data;

@Data
public class UserProfileDto {
    private String id;
    private String username;
    private String email;
    private String bio;
}
