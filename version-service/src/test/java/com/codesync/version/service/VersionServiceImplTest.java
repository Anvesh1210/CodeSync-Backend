package com.codesync.version.service;

import com.codesync.version.dto.DiffResult;
import com.codesync.version.dto.SnapshotRequest;
import com.codesync.version.entity.Snapshot;
import com.codesync.version.repository.SnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionServiceImplTest {

    @Mock
    private SnapshotRepository snapshotRepository;

    @InjectMocks
    private VersionServiceImpl versionService;

    private UUID snapshotId;
    private String fileId;
    private Snapshot mockSnapshot;

    @BeforeEach
    void setUp() {
        snapshotId = UUID.randomUUID();
        fileId = "file-123";
        mockSnapshot = Snapshot.builder()
                .snapshotId(snapshotId)
                .fileId(fileId)
                .content("line 1\nline 2")
                .branch("main")
                .build();
    }

    @Test
    void createSnapshot_ShouldReturnSnapshot() {
        SnapshotRequest request = new SnapshotRequest();
        request.setFileId(fileId);
        request.setContent("new content");
        
        when(snapshotRepository.findFirstByFileIdOrderByCreatedAtDesc(anyString())).thenReturn(Optional.empty());
        when(snapshotRepository.save(any(Snapshot.class))).thenReturn(mockSnapshot);

        Snapshot result = versionService.createSnapshot(request);
        assertNotNull(result);
    }

    @Test
    void getSnapshotById_ShouldReturnSnapshot() {
        when(snapshotRepository.findById(snapshotId)).thenReturn(Optional.of(mockSnapshot));
        Snapshot result = versionService.getSnapshotById(snapshotId);
        assertNotNull(result);
        assertEquals(snapshotId, result.getSnapshotId());
    }

    @Test
    void getSnapshotsByFile_ShouldReturnList() {
        when(snapshotRepository.findByFileIdOrderByCreatedAtDesc(fileId)).thenReturn(Arrays.asList(mockSnapshot));
        List<Snapshot> result = versionService.getSnapshotsByFile(fileId);
        assertEquals(1, result.size());
    }

    @Test
    void diffSnapshots_WithDifferences_ShouldReturnDiffs() {
        UUID newId = UUID.randomUUID();
        Snapshot newSnapshot = Snapshot.builder()
                .snapshotId(newId)
                .content("line 1\nline 3\nline 4")
                .build();

        when(snapshotRepository.findById(snapshotId)).thenReturn(Optional.of(mockSnapshot));
        when(snapshotRepository.findById(newId)).thenReturn(Optional.of(newSnapshot));

        DiffResult result = versionService.diffSnapshots(snapshotId, newId);

        assertNotNull(result);
        assertNotNull(result.getDifferences());
    }

    @Test
    void createBranch_ShouldUpdateBranch() {
        when(snapshotRepository.findById(snapshotId)).thenReturn(Optional.of(mockSnapshot));
        when(snapshotRepository.save(any(Snapshot.class))).thenReturn(mockSnapshot);
        Snapshot result = versionService.createBranch(snapshotId, "feature-1");
        assertEquals("feature-1", result.getBranch());
    }

    @Test
    void tagSnapshot_ShouldUpdateTag() {
        when(snapshotRepository.findById(snapshotId)).thenReturn(Optional.of(mockSnapshot));
        when(snapshotRepository.save(any(Snapshot.class))).thenReturn(mockSnapshot);
        Snapshot result = versionService.tagSnapshot(snapshotId, "v1.0");
        assertEquals("v1.0", result.getTag());
    }

    @Test
    void getFileHistory_ShouldReturnList() {
        when(snapshotRepository.findByFileIdOrderByCreatedAtDesc(fileId)).thenReturn(Arrays.asList(mockSnapshot));
        List<Snapshot> result = versionService.getFileHistory(fileId);
        assertEquals(1, result.size());
    }

    @Test
    void getLatestSnapshot_ShouldThrowException_WhenNotFound() {
        when(snapshotRepository.findFirstByFileIdOrderByCreatedAtDesc(anyString())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> versionService.getLatestSnapshot("none"));
    }

    @Test
    void restoreSnapshot_ShouldReturnSnapshot() {
        when(snapshotRepository.findById(snapshotId)).thenReturn(Optional.of(mockSnapshot));
        when(snapshotRepository.save(any(Snapshot.class))).thenReturn(mockSnapshot);
        Snapshot result = versionService.restoreSnapshot(snapshotId, "user-1");
        assertNotNull(result);
        assertEquals(snapshotId, result.getSnapshotId());
    }
}
