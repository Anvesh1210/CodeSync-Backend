package com.codesync.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents one node in the file tree.
 * Folders have children; files have children = empty list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileTreeNode {

    private UUID fileId;
    private String name;
    private String path;
    private String language;
    private Boolean isFolder;
    private UUID parentId;

    @Builder.Default
    private List<FileTreeNode> children = new ArrayList<>();
}
