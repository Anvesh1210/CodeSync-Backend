package com.codesync.version.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiffResult {
    private UUID oldSnapshotId;
    private UUID newSnapshotId;
    private List<String> differences;
}
