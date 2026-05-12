package com.codesync.file.service;

import com.codesync.file.dto.FileContentRequest;
import com.codesync.file.dto.FileRequest;
import com.codesync.file.dto.FileResponse;
import com.codesync.file.dto.FileTreeNode;

import java.util.List;
import java.util.UUID;

public interface FileService {

    FileResponse createFile(FileRequest request);

    FileResponse getFileById(UUID fileId);

    List<FileResponse> getFilesByProject(UUID projectId);

    String getFileContent(UUID fileId);

    FileResponse updateFileContent(UUID fileId, FileContentRequest request);

    FileResponse renameFile(UUID fileId, String newName, UUID userId);

    void deleteFile(UUID fileId, UUID userId);

    FileResponse restoreFile(UUID fileId);

    FileResponse moveFile(UUID fileId, String newPath, UUID userId);

    FileResponse createFolder(FileRequest request);

    List<FileTreeNode> getFileTree(UUID projectId);

    List<FileResponse> searchInProject(UUID projectId, String query);
}
