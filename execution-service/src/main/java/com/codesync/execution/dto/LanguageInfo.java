package com.codesync.execution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageInfo {

    private String name;
    private String version;
    private String displayName;
    private String extension;
    private String boilerplate;
    private String defaultFileName;
}
