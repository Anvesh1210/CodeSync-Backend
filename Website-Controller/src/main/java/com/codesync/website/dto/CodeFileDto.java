package com.codesync.website.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class CodeFileDto {
    @JsonAlias("id")
    private String fileId;
    private String projectId;
    private String name;
    private String path;
    private String language;
    private String content;
    private Boolean isFolder;
    private Boolean deleted;
    private String parentId;
    private String userId;
    private Boolean isPremium;
    private String createdAt;
    private String updatedAt;
}
