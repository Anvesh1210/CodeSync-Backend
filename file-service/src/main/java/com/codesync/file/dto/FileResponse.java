package com.codesync.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {

    private UUID fileId;
    private UUID projectId;
    private String name;
    private String path;
    private String language;
    private String content;
    private UUID createdById;
    private UUID lastEditedBy;
    private boolean isDeleted;
    private UUID parentId;
    private Boolean isFolder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
