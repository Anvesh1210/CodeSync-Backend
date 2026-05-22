package com.codesync.website.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
