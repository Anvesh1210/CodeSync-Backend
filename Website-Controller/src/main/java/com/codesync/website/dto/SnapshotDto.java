package com.codesync.website.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SnapshotDto {
    private String id;
    private String projectId;
    private String tag;
    private LocalDateTime timestamp;
    private String commitMessage;
}
