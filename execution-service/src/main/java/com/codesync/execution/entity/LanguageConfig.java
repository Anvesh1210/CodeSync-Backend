package com.codesync.execution.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "supported_languages")
public class LanguageConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String extension;

    @Column(nullable = false)
    private String dockerImage;

    private String compileCommand;

    @Column(nullable = false)
    private String runCommand;

    private int timeoutSeconds = 10;

    private String memoryLimit = "256m";

    private String versionCommand;

    private String defaultFileName;

    private boolean isInterpreted;

    @Column(columnDefinition = "TEXT")
    private String boilerplate;
}
