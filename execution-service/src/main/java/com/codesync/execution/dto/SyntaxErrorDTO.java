package com.codesync.execution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyntaxErrorDTO {
    private int line;
    private int column;
    private String message;
    private String severity; // e.g., "error", "warning"
}
