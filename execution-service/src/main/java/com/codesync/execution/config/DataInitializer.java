package com.codesync.execution.config;

import com.codesync.execution.entity.LanguageConfig;
import com.codesync.execution.repository.LanguageRepository;
import com.codesync.execution.service.LanguageRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final LanguageRepository languageRepository;
    private final LanguageRegistry languageRegistry;

    @Override
    public void run(String... args) throws Exception {
        if (languageRepository.count() == 0) {
            log.info("No languages found in database. Initializing default languages...");
            
            languageRepository.save(LanguageConfig.builder()
                    .name("java")
                    .extension("java")
                    .dockerImage("eclipse-temurin:17-jdk-jammy")
                    .compileCommand("javac Main.java")
                    .runCommand("java Main")
                    .timeoutSeconds(15)
                    .memoryLimit("512m")
                    .versionCommand("java -version")
                    .defaultFileName("Main.java")
                    .isInterpreted(false)
                    .boilerplate("public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World\");\n    }\n}")
                    .build());

            languageRepository.save(LanguageConfig.builder()
                    .name("python")
                    .extension("py")
                    .dockerImage("python:3.10-slim")
                    .runCommand("python main.py")
                    .timeoutSeconds(10)
                    .memoryLimit("256m")
                    .versionCommand("python --version")
                    .defaultFileName("main.py")
                    .isInterpreted(true)
                    .boilerplate("print(\"Hello World\")")
                    .build());

            languageRepository.save(LanguageConfig.builder()
                    .name("cpp")
                    .extension("cpp")
                    .dockerImage("gcc:latest")
                    .compileCommand("g++ main.cpp -o main")
                    .runCommand("./main")
                    .timeoutSeconds(10)
                    .memoryLimit("256m")
                    .versionCommand("g++ --version")
                    .defaultFileName("main.cpp")
                    .isInterpreted(false)
                    .boilerplate("#include <iostream>\n\nint main() {\n    std::cout << \"Hello World\" << std::endl;\n    return 0;\n}")
                    .build());

            languageRepository.save(LanguageConfig.builder()
                    .name("javascript")
                    .extension("js")
                    .dockerImage("node:18-slim")
                    .runCommand("node main.js")
                    .timeoutSeconds(10)
                    .memoryLimit("256m")
                    .versionCommand("node -v")
                    .defaultFileName("main.js")
                    .isInterpreted(true)
                    .boilerplate("console.log(\"Hello World\");")
                    .build());

            log.info("Default languages initialized.");
            languageRegistry.refreshCache();
        } else {
            log.info("Languages already exist in database. Found: {}", 
                languageRepository.findAll().stream().map(LanguageConfig::getName).toList());
            log.info("Skipping initialization.");
        }
    }
}
