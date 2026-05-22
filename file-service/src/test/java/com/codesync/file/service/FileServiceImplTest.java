package com.codesync.file.service;

import com.codesync.file.dto.FileContentRequest;
import com.codesync.file.dto.FileRequest;
import com.codesync.file.dto.FileResponse;
import com.codesync.file.dto.FileTreeNode;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.entity.CodeFolder;
import com.codesync.file.repository.FileRepository;
import com.codesync.file.repository.FolderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FolderRepository folderRepository;

    @InjectMocks
    private FileServiceImpl fileService;

    private UUID projectId;
    private UUID userId;
    private UUID fileId;
    private UUID folderId;
    private CodeFile mockFile;
    private CodeFolder mockFolder;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        fileId = UUID.randomUUID();
        folderId = UUID.randomUUID();

        mockFile = CodeFile.builder()
                .fileId(fileId)
                .projectId(projectId)
                .name("test.js")
                .path("/test.js")
                .language("javascript")
                .content("console.log('hello');")
                .createdById(userId)
                .lastEditedBy(userId)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        mockFolder = CodeFolder.builder()
                .folderId(folderId)
                .projectId(projectId)
                .name("src")
                .path("/src")
                .createdById(userId)
                .lastEditedBy(userId)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createFile_ShouldSucceed_WhenWithinLimit() {
        FileRequest request = new FileRequest();
        request.setProjectId(projectId);
        request.setName("new.js");
        request.setPath("/new.js");
        request.setLanguage("javascript");
        request.setContent("const x = 1;");
        request.setUserId(userId);
        request.setPremium(false);

        when(fileRepository.findByProjectIdAndIsDeleted(projectId, false)).thenReturn(Collections.emptyList());
        when(fileRepository.save(any(CodeFile.class))).thenReturn(mockFile);

        FileResponse response = fileService.createFile(request);

        assertNotNull(response);
        assertEquals(fileId, response.getFileId());
        verify(fileRepository).save(any(CodeFile.class));
    }

    @Test
    void createFile_ShouldThrowException_WhenLimitReached() {
        FileRequest request = new FileRequest();
        request.setProjectId(projectId);
        request.setContent("a".repeat(11 * 1024 * 1024)); // 11MB
        request.setPremium(false); // 10MB limit

        when(fileRepository.findByProjectIdAndIsDeleted(projectId, false)).thenReturn(Collections.emptyList());

        assertThrows(RuntimeException.class, () -> fileService.createFile(request));
    }

    @Test
    void getFileById_ShouldReturnFile_WhenExists() {
        when(fileRepository.findByFileId(fileId)).thenReturn(Optional.of(mockFile));

        FileResponse response = fileService.getFileById(fileId);

        assertNotNull(response);
        assertEquals("test.js", response.getName());
        assertFalse(response.getIsFolder());
    }

    @Test
    void getFileById_ShouldReturnFolder_WhenFileNotExistsButFolderDoes() {
        when(fileRepository.findByFileId(folderId)).thenReturn(Optional.empty());
        when(folderRepository.findByFolderId(folderId)).thenReturn(Optional.of(mockFolder));

        FileResponse response = fileService.getFileById(folderId);

        assertNotNull(response);
        assertEquals("src", response.getName());
        assertTrue(response.getIsFolder());
    }

    @Test
    void getFileById_ShouldThrowException_WhenNeitherExists() {
        when(fileRepository.findByFileId(fileId)).thenReturn(Optional.empty());
        when(folderRepository.findByFolderId(fileId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> fileService.getFileById(fileId));
    }

    @Test
    void getFilesByProject_ShouldReturnAll() {
        when(folderRepository.findByProjectIdAndIsDeleted(projectId, false)).thenReturn(List.of(mockFolder));
        when(fileRepository.findByProjectIdAndIsDeleted(projectId, false)).thenReturn(List.of(mockFile));

        List<FileResponse> responses = fileService.getFilesByProject(projectId);

        assertEquals(2, responses.size());
    }

    @Test
    void getFileContent_ShouldReturnContent() {
        when(fileRepository.findByFileId(fileId)).thenReturn(Optional.of(mockFile));

        String content = fileService.getFileContent(fileId);

        assertEquals("console.log('hello');", content);
    }

    @Test
    void updateFileContent_ShouldSucceed() {
        FileContentRequest request = new FileContentRequest();
        request.setContent("new content");
        request.setUserId(userId);
        request.setPremium(true);

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(mockFile));
        when(fileRepository.findByProjectIdAndIsDeleted(projectId, false)).thenReturn(List.of(mockFile));
        when(fileRepository.save(any(CodeFile.class))).thenReturn(mockFile);

        FileResponse response = fileService.updateFileContent(fileId, request);

        assertNotNull(response);
        verify(fileRepository).save(any(CodeFile.class));
    }

    @Test
    void renameFile_ShouldRenameFile() {
        when(fileRepository.findByFileId(fileId)).thenReturn(Optional.of(mockFile));
        when(fileRepository.save(any(CodeFile.class))).thenReturn(mockFile);

        FileResponse response = fileService.renameFile(fileId, "newname.js", userId);

        assertEquals("newname.js", response.getName());
        verify(fileRepository).save(any(CodeFile.class));
    }

    @Test
    void renameFile_ShouldRenameFolder_WhenFileNotExists() {
        when(fileRepository.findByFileId(folderId)).thenReturn(Optional.empty());
        when(folderRepository.findByFolderId(folderId)).thenReturn(Optional.of(mockFolder));
        when(folderRepository.save(any(CodeFolder.class))).thenReturn(mockFolder);

        FileResponse response = fileService.renameFile(folderId, "newsrc", userId);

        assertTrue(response.getIsFolder());
        verify(folderRepository).save(any(CodeFolder.class));
    }

    @Test
    void deleteFile_ShouldDeleteFile() {
        when(fileRepository.findByFileId(fileId)).thenReturn(Optional.of(mockFile));

        fileService.deleteFile(fileId, userId);

        assertTrue(mockFile.isDeleted());
        verify(fileRepository).save(mockFile);
    }

    @Test
    void deleteFile_ShouldDeleteFolder_WhenFileNotExists() {
        when(fileRepository.findByFileId(folderId)).thenReturn(Optional.empty());
        when(folderRepository.findByFolderId(folderId)).thenReturn(Optional.of(mockFolder));

        fileService.deleteFile(folderId, userId);

        assertTrue(mockFolder.isDeleted());
        verify(folderRepository).save(mockFolder);
    }

    @Test
    void restoreFile_ShouldRestoreFile() {
        mockFile.setDeleted(true);
        when(fileRepository.findByFileId(fileId)).thenReturn(Optional.of(mockFile));
        when(fileRepository.save(any(CodeFile.class))).thenReturn(mockFile);

        FileResponse response = fileService.restoreFile(fileId);

        assertFalse(mockFile.isDeleted());
        verify(fileRepository).save(mockFile);
    }

    @Test
    void moveFile_ShouldMoveFile() {
        when(fileRepository.findByFileId(fileId)).thenReturn(Optional.of(mockFile));
        when(fileRepository.save(any(CodeFile.class))).thenReturn(mockFile);

        fileService.moveFile(fileId, "/new/path/test.js", userId);

        assertEquals("/new/path/test.js", mockFile.getPath());
        verify(fileRepository).save(mockFile);
    }

    @Test
    void createFolder_ShouldSucceed() {
        FileRequest request = new FileRequest();
        request.setProjectId(projectId);
        request.setName("newfolder");
        request.setUserId(userId);

        when(folderRepository.save(any(CodeFolder.class))).thenReturn(mockFolder);

        FileResponse response = fileService.createFolder(request);

        assertNotNull(response);
        assertTrue(response.getIsFolder());
    }

    @Test
    void getFileTree_ShouldBuildTree() {
        UUID subFolderId = UUID.randomUUID();
        CodeFolder subFolder = CodeFolder.builder()
                .folderId(subFolderId)
                .name("sub")
                .parentFolderId(folderId)
                .build();
        
        CodeFile subFile = CodeFile.builder()
                .fileId(UUID.randomUUID())
                .name("subfile.js")
                .folderId(subFolderId)
                .build();

        when(fileRepository.findByProjectIdAndIsDeleted(projectId, false)).thenReturn(List.of(mockFile, subFile));
        when(folderRepository.findByProjectIdAndIsDeleted(projectId, false)).thenReturn(List.of(mockFolder, subFolder));

        List<FileTreeNode> tree = fileService.getFileTree(projectId);

        assertNotNull(tree);
        // roots: mockFile (no folder), mockFolder (no parent)
        assertEquals(2, tree.size());
    }

    @Test
    void searchInProject_ShouldReturnResults() {
        when(fileRepository.searchInProject(projectId, "test")).thenReturn(List.of(mockFile));

        List<FileResponse> results = fileService.searchInProject(projectId, "test");

        assertEquals(1, results.size());
    }

    @Test
    void searchInProject_ShouldReturnEmpty_WhenQueryBlank() {
        List<FileResponse> results = fileService.searchInProject(projectId, " ");
        assertTrue(results.isEmpty());
    }
}
