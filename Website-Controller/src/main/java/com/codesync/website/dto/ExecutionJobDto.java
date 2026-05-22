package com.codesync.website.dto;

import lombok.Data;

@Data
public class ExecutionJobDto {
    private String id;
    private String code;
    private String language;
    private String status;
    private String output;
    private String error;
}
