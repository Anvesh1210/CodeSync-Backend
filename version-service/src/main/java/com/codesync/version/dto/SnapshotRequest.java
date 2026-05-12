package com.codesync.version.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotRequest {
    private String projectId;
    private String fileId;
    private String authorId;
    private String message;
    private String content;
    private String branch;
}
