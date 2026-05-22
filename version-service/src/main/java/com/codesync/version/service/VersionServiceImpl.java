package com.codesync.version.service;

import com.codesync.version.dto.DiffResult;
import com.codesync.version.dto.SnapshotRequest;
import com.codesync.version.entity.Snapshot;
import com.codesync.version.repository.SnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VersionServiceImpl implements VersionService {

    @Autowired
    private SnapshotRepository snapshotRepository;

    @Override
    public Snapshot createSnapshot(SnapshotRequest request) {
        String content = request.getContent() != null ? request.getContent() : "";
        String hash = generateHash(content);

        UUID parentId = null;
        Optional<Snapshot> latest = snapshotRepository.findFirstByFileIdOrderByCreatedAtDesc(request.getFileId());
        if (latest.isPresent()) {
            parentId = latest.get().getSnapshotId();
        }

        Snapshot snapshot = Snapshot.builder()
                .projectId(request.getProjectId())
                .fileId(request.getFileId())
                .authorId(request.getAuthorId())
                .message(request.getMessage())
                .content(content)
                .hash(hash)
                .parentSnapshotId(parentId)
                .branch(request.getBranch() != null ? request.getBranch() : "main")
                .createdAt(LocalDateTime.now())
                .build();
                
        return snapshotRepository.save(snapshot);
    }

    @Override
    public Snapshot getSnapshotById(UUID snapshotId) {
        return snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new RuntimeException("Snapshot not found"));
    }

    @Override
    public List<Snapshot> getSnapshotsByFile(String fileId) {
        return snapshotRepository.findByFileIdOrderByCreatedAtDesc(fileId);
    }

    @Override
    public List<Snapshot> getSnapshotsByProject(String projectId) {
        return snapshotRepository.findByProjectId(projectId);
    }

    @Override
    public List<Snapshot> getSnapshotsByBranch(String branch) {
        return snapshotRepository.findByBranch(branch);
    }

    @Override
    public Snapshot getLatestSnapshot(String fileId) {
        return snapshotRepository.findFirstByFileIdOrderByCreatedAtDesc(fileId)
                .orElseThrow(() -> new RuntimeException("No snapshots found for file"));
    }

    @Override
    public Snapshot restoreSnapshot(UUID snapshotId, String authorId) {
        Snapshot oldSnapshot = getSnapshotById(snapshotId);
        
        Snapshot newSnapshot = Snapshot.builder()
                .projectId(oldSnapshot.getProjectId())
                .fileId(oldSnapshot.getFileId())
                .authorId(authorId)
                .message("Reverted to snapshot " + snapshotId)
                .content(oldSnapshot.getContent())
                .hash(oldSnapshot.getHash())
                .parentSnapshotId(oldSnapshot.getSnapshotId())
                .branch(oldSnapshot.getBranch())
                .createdAt(LocalDateTime.now())
                .build();
                
        return snapshotRepository.save(newSnapshot);
    }

    @Override
    public DiffResult diffSnapshots(UUID oldSnapshotId, UUID newSnapshotId) {
        Snapshot oldSnapshot = getSnapshotById(oldSnapshotId);
        Snapshot newSnapshot = getSnapshotById(newSnapshotId);
        
        List<String> oldLines = Arrays.asList(oldSnapshot.getContent().split("\\r?\\n"));
        List<String> newLines = Arrays.asList(newSnapshot.getContent().split("\\r?\\n"));
        
        // Simplified Diff algorithm implementation
        List<String> diffs = new ArrayList<>();
        int i = 0, j = 0;
        
        while (i < oldLines.size() && j < newLines.size()) {
            if (oldLines.get(i).equals(newLines.get(j))) {
                diffs.add("  " + oldLines.get(i));
                i++;
                j++;
            } else {
                diffs.add("- " + oldLines.get(i));
                diffs.add("+ " + newLines.get(j));
                i++;
                j++;
            }
        }
        
        while (i < oldLines.size()) {
            diffs.add("- " + oldLines.get(i));
            i++;
        }
        
        while (j < newLines.size()) {
            diffs.add("+ " + newLines.get(j));
            j++;
        }
        
        return new DiffResult(oldSnapshotId, newSnapshotId, diffs);
    }

    @Override
    public Snapshot createBranch(UUID snapshotId, String newBranch) {
        Snapshot snapshot = getSnapshotById(snapshotId);
        snapshot.setBranch(newBranch);
        return snapshotRepository.save(snapshot);
    }

    @Override
    public Snapshot tagSnapshot(UUID snapshotId, String tag) {
        Snapshot snapshot = getSnapshotById(snapshotId);
        snapshot.setTag(tag);
        return snapshotRepository.save(snapshot);
    }

    @Override
    public List<Snapshot> getFileHistory(String fileId) {
        return snapshotRepository.findByFileIdOrderByCreatedAtDesc(fileId);
    }
    
    private String generateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating hash", e);
        }
    }
}
