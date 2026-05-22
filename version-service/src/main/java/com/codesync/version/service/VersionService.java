package com.codesync.version.service;

import com.codesync.version.dto.DiffResult;
import com.codesync.version.dto.SnapshotRequest;
import com.codesync.version.entity.Snapshot;

import java.util.List;

import java.util.UUID;

public interface VersionService {
    Snapshot createSnapshot(SnapshotRequest request);
    Snapshot getSnapshotById(UUID snapshotId);
    List<Snapshot> getSnapshotsByFile(String fileId);
    List<Snapshot> getSnapshotsByProject(String projectId);
    List<Snapshot> getSnapshotsByBranch(String branch);
    Snapshot getLatestSnapshot(String fileId);
    Snapshot restoreSnapshot(UUID snapshotId, String authorId);
    DiffResult diffSnapshots(UUID oldSnapshotId, UUID newSnapshotId);
    Snapshot createBranch(UUID snapshotId, String newBranch);
    Snapshot tagSnapshot(UUID snapshotId, String tag);
    List<Snapshot> getFileHistory(String fileId);
}
