package com.codesync.website.dto;

import lombok.Data;

@Data
public class CommentDto {
    private String id;
    private String projectId;
    private String authorId;
    private String content;
    private boolean resolved;
}
