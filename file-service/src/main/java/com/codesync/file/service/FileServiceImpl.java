package com.codesync.file.service;

import com.codesync.file.dto.FileContentRequest;
import com.codesync.file.dto.FileRequest;
import com.codesync.file.dto.FileResponse;
import com.codesync.file.dto.FileTreeNode;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.entity.CodeFolder;
import com.codesync.file.repository.FileRepository;
import com.codesync.file.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;

    @Override
    @Transactional
    public FileResponse createFile(FileRequest request) {
        log.info("Creating file '{}' in project {}", request.getName(), request.getProjectId());
        
        long limit = request.isPremium() ? 100 * 1024 * 1024 : 10 * 1024 * 1024;
        long currentSize = calculateProjectStorage(request.getProjectId());
        long newFileSize = request.getContent() != null ? request.getContent().length() : 0;
        
        if (currentSize + newFileSize > limit) {
            log.warn("Storage limit reached for project {}: {}/{}", request.getProjectId(), currentSize + newFileSize, limit);
            throw new RuntimeException("Storage limit reached (" + (limit / (1024 * 1024)) + "MB). Upgrade to Premium for more storage.");
        }

        CodeFile file = CodeFile.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .path(request.getPath())
                .language(request.getLanguage())
                .content(request.getContent() != null ? request.getContent() : "")
                .createdById(request.getUserId())
                .lastEditedBy(request.getUserId())
                .folderId(request.getParentId())
                .isDeleted(false)
                .build();
        return toResponse(fileRepository.save(file));
    }

    private long calculateProjectStorage(UUID projectId) {
        return fileRepository.findByProjectIdAndIsDeleted(projectId, false)
                .stream()
                .mapToLong(f -> f.getContent() != null ? f.getContent().length() : 0)
                .sum();
    }

    @Override
    public FileResponse getFileById(UUID fileId) {
        log.info("Fetching file {}", fileId);
        return fileRepository.findByFileId(fileId)
                .filter(f -> !f.isDeleted())
                .map(this::toResponse)
                .orElseGet(() -> folderRepository.findByFolderId(fileId)
                        .filter(f -> !f.isDeleted())
                        .map(this::toResponse)
                        .orElseThrow(() -> new RuntimeException("File/Folder not found: " + fileId)));
    }

    @Override
    public List<FileResponse> getFilesByProject(UUID projectId) {
        log.info("Fetching all files and folders for project {}", projectId);
        List<FileResponse> responses = new ArrayList<>();
        folderRepository.findByProjectIdAndIsDeleted(projectId, false)
                .forEach(f -> responses.add(toResponse(f)));
        fileRepository.findByProjectIdAndIsDeleted(projectId, false)
                .forEach(f -> responses.add(toResponse(f)));
        return responses;
    }

    @Override
    public String getFileContent(UUID fileId) {
        log.info("Fetching content for file {}", fileId);
        return fileRepository.findByFileId(fileId)
                .filter(f -> !f.isDeleted())
                .map(CodeFile::getContent)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
    }

    @Override
    @Transactional
    public FileResponse updateFileContent(UUID fileId, FileContentRequest request) {
        CodeFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
        
        long limit = request.isPremium() ? 100 * 1024 * 1024 : 10 * 1024 * 1024;
        long currentSize = calculateProjectStorage(file.getProjectId());
        long oldFileSize = file.getContent() != null ? file.getContent().length() : 0;
        long newFileSize = request.getContent() != null ? request.getContent().length() : 0;
        
        if (currentSize - oldFileSize + newFileSize > limit) {
            throw new RuntimeException("Storage limit reached (" + (limit / (1024 * 1024)) + "MB). Upgrade to Premium for more storage.");
        }

        file.setContent(request.getContent());
        file.setLastEditedBy(request.getUserId());
        return toResponse(fileRepository.save(file));
    }

    @Override
    @Transactional
    public FileResponse renameFile(UUID fileId, String newName, UUID userId) {
        log.info("Renaming file/folder {} to '{}'", fileId, newName);
        return fileRepository.findByFileId(fileId).filter(f -> !f.isDeleted()).map(file -> {
            file.setName(newName);
            file.setLastEditedBy(userId);
            return toResponse(fileRepository.save(file));
        }).orElseGet(() -> {
            CodeFolder folder = folderRepository.findByFolderId(fileId)
                    .filter(f -> !f.isDeleted())
                    .orElseThrow(() -> new RuntimeException("File/Folder not found: " + fileId));
            folder.setName(newName);
            folder.setLastEditedBy(userId);
            return toResponse(folderRepository.save(folder));
        });
    }

    @Override
    @Transactional
    public void deleteFile(UUID fileId, UUID userId) {
        log.info("Soft-deleting file/folder {} by user {}", fileId, userId);
        fileRepository.findByFileId(fileId).filter(f -> !f.isDeleted()).ifPresentOrElse(file -> {
            file.setDeleted(true);
            file.setLastEditedBy(userId);
            fileRepository.save(file);
        }, () -> {
            CodeFolder folder = folderRepository.findByFolderId(fileId)
                    .filter(f -> !f.isDeleted())
                    .orElseThrow(() -> new RuntimeException("File/Folder not found: " + fileId));
            folder.setDeleted(true);
            folder.setLastEditedBy(userId);
            folderRepository.save(folder);
        });
    }

    @Override
    @Transactional
    public FileResponse restoreFile(UUID fileId) {
        log.info("Restoring file/folder {}", fileId);
        return fileRepository.findByFileId(fileId).map(file -> {
            file.setDeleted(false);
            return toResponse(fileRepository.save(file));
        }).orElseGet(() -> {
            CodeFolder folder = folderRepository.findByFolderId(fileId)
                    .orElseThrow(() -> new RuntimeException("File/Folder not found: " + fileId));
            folder.setDeleted(false);
            return toResponse(folderRepository.save(folder));
        });
    }

    @Override
    @Transactional
    public FileResponse moveFile(UUID fileId, String newPath, UUID userId) {
        log.info("Moving file/folder {} to path '{}'", fileId, newPath);
        return fileRepository.findByFileId(fileId).filter(f -> !f.isDeleted()).map(file -> {
            file.setPath(newPath);
            file.setLastEditedBy(userId);
            return toResponse(fileRepository.save(file));
        }).orElseGet(() -> {
            CodeFolder folder = folderRepository.findByFolderId(fileId)
                    .filter(f -> !f.isDeleted())
                    .orElseThrow(() -> new RuntimeException("File/Folder not found: " + fileId));
            folder.setPath(newPath);
            folder.setLastEditedBy(userId);
            return toResponse(folderRepository.save(folder));
        });
    }

    @Override
    @Transactional
    public FileResponse createFolder(FileRequest request) {
        log.info("Creating folder '{}' in project {} with parent {}", request.getName(), request.getProjectId(), request.getParentId());
        CodeFolder folder = CodeFolder.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .path(request.getPath())
                .createdById(request.getUserId())
                .lastEditedBy(request.getUserId())
                .parentFolderId(request.getParentId())
                .isDeleted(false)
                .build();
        CodeFolder savedFolder = folderRepository.save(folder);
        log.info("Folder created with ID: {}", savedFolder.getFolderId());
        return toResponse(savedFolder);
    }

    @Override
    public List<FileTreeNode> getFileTree(UUID projectId) {
        log.info("Building file tree for project {}", projectId);
        List<CodeFile> allFiles = fileRepository.findByProjectIdAndIsDeleted(projectId, false);
        List<CodeFolder> allFolders = folderRepository.findByProjectIdAndIsDeleted(projectId, false);

        Map<UUID, FileTreeNode> nodeMap = new HashMap<>();

        for (CodeFolder f : allFolders) {
            nodeMap.put(f.getFolderId(), FileTreeNode.builder()
                    .fileId(f.getFolderId())
                    .name(f.getName())
                    .path(f.getPath())
                    .language(null)
                    .isFolder(true)
                    .parentId(f.getParentFolderId())
                    .children(new ArrayList<>())
                    .build());
        }

        for (CodeFile f : allFiles) {
            nodeMap.put(f.getFileId(), FileTreeNode.builder()
                    .fileId(f.getFileId())
                    .name(f.getName())
                    .path(f.getPath())
                    .language(f.getLanguage())
                    .isFolder(false)
                    .parentId(f.getFolderId())
                    .children(new ArrayList<>())
                    .build());
        }

        List<FileTreeNode> roots = new ArrayList<>();

        for (CodeFolder folder : allFolders) {
            FileTreeNode node = nodeMap.get(folder.getFolderId());
            if (folder.getParentFolderId() == null) {
                roots.add(node);
            } else {
                FileTreeNode parent = nodeMap.get(folder.getParentFolderId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }

        for (CodeFile file : allFiles) {
            FileTreeNode node = nodeMap.get(file.getFileId());
            if (file.getFolderId() == null) {
                roots.add(node);
            } else {
                FileTreeNode parent = nodeMap.get(file.getFolderId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        
        log.info("Built tree with {} root nodes for project {}", roots.size(), projectId);
        return roots;
    }

    @Override
    public List<FileResponse> searchInProject(UUID projectId, String query) {
        log.info("Searching '{}' in project {}", query, projectId);
        if (query == null || query.isBlank()) return List.of();
        List<FileResponse> responses = new ArrayList<>();
        
        // Assuming searchInProject on fileRepository works on names and content
        fileRepository.searchInProject(projectId, query.trim())
                .forEach(f -> responses.add(toResponse(f)));
                
        return responses;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private FileResponse toResponse(CodeFile file) {
        return FileResponse.builder()
                .fileId(file.getFileId())
                .projectId(file.getProjectId())
                .name(file.getName())
                .path(file.getPath())
                .language(file.getLanguage())
                .content(file.getContent())
                .createdById(file.getCreatedById())
                .lastEditedBy(file.getLastEditedBy())
                .isDeleted(file.isDeleted())
                .parentId(file.getFolderId())
                .isFolder(false)
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }

    private FileResponse toResponse(CodeFolder folder) {
        return FileResponse.builder()
                .fileId(folder.getFolderId())
                .projectId(folder.getProjectId())
                .name(folder.getName())
                .path(folder.getPath())
                .language(null)
                .content(null)
                .createdById(folder.getCreatedById())
                .lastEditedBy(folder.getLastEditedBy())
                .isDeleted(folder.isDeleted())
                .parentId(folder.getParentFolderId())
                .isFolder(true)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }
}
