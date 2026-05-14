package com.codesync.execution.service;

import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.LanguageConfig;
import com.codesync.execution.dto.ExecutionRequest;
import com.codesync.execution.dto.SyntaxErrorDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerService {

    private final LanguageRegistry languageRegistry;

    public void executeJob(ExecutionJob job, boolean isPremium, Consumer<String> outputStream) {
        log.info("Executing job {} | language={} | isPremium={}", job.getJobId(), job.getLanguage(), isPremium);
        long start = System.currentTimeMillis();

        LanguageConfig config = languageRegistry.getConfig(job.getLanguage())
                .orElseThrow(() -> new RuntimeException("Unsupported language: " + job.getLanguage()));

        int timeoutSeconds = isPremium ? config.getTimeoutSeconds() * 2 : config.getTimeoutSeconds();
        String memoryLimit = isPremium ? "512m" : config.getMemoryLimit();

        try {
            Path tempDir = Files.createTempDirectory("codesync-job-" + job.getJobId());
            String fileName = job.getFileName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = config.getDefaultFileName() != null ? config.getDefaultFileName() : "main." + config.getExtension();
            }
            Path sourceFile = tempDir.resolve(fileName);
            Files.writeString(sourceFile, job.getSourceCode());

            // Write stdin to input.txt
            Path inputFile = tempDir.resolve("input.txt");
            Files.writeString(inputFile, job.getStdin() != null ? job.getStdin() : "");

            String[] command = buildDockerCommand(config, tempDir.toAbsolutePath().toString(), fileName, memoryLimit);
            
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread outThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                        if (outputStream != null) outputStream.accept(line + "\n");
                    }
                } catch (IOException ignored) {}
            });

            Thread errThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                        if (outputStream != null) outputStream.accept("ERR: " + line + "\n");
                    }
                } catch (IOException ignored) {}
            });

            outThread.start();
            errThread.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                job.setStatus(com.codesync.execution.entity.JobStatus.TIME_OUT);
                job.setStderr("Execution timed out (" + timeoutSeconds + "s)");
                if (outputStream != null) outputStream.accept("\n[EXECUTION TIMED OUT]");
            } else {
                outThread.join(1000);
                errThread.join(1000);
                job.setStdout(stdout.toString());
                job.setStderr(stderr.toString());
            }

            // Cleanup
            org.springframework.util.FileSystemUtils.deleteRecursively(tempDir);

        } catch (Exception e) {
            log.error("Docker execution error: {}", e.getMessage());
            job.setStderr("Docker execution error: " + e.getMessage());
            job.setStatus(com.codesync.execution.entity.JobStatus.FAILED);
        } finally {
            job.setExecutionTimeMs(System.currentTimeMillis() - start);
            job.setMemoryUsedKb(2048L); // Simulated
        }
    }

    private String[] buildDockerCommand(LanguageConfig config, String dir, String file, String memoryLimit) {
        String workDir = "/app";
        // Convert Windows backslashes to forward slashes for Docker volume mapping
        String normalizedDir = dir.replace("\\", "/");
        // For Docker on Windows (Git Bash/WSL), sometimes it needs a leading slash or drive letter conversion
        // But usually C:/path works for Docker Desktop.
        String volume = normalizedDir + ":" + workDir;
        
        List<String> cmdList = new ArrayList<>();
        cmdList.add("docker");
        cmdList.add("run");
        cmdList.add("--rm");
        cmdList.add("--memory=" + memoryLimit);
        cmdList.add("--cpus=1");
        cmdList.add("--network=none");
        cmdList.add("--pids-limit=100");
        cmdList.add("--cap-drop=ALL");
        cmdList.add("--security-opt=no-new-privileges");
        
        cmdList.add("-v");
        cmdList.add(volume);
        cmdList.add("-w");
        cmdList.add(workDir);
        cmdList.add(config.getDockerImage());
        cmdList.add("sh");
        cmdList.add("-c");
        
        String fileNoExt = file.contains(".") ? file.substring(0, file.lastIndexOf('.')) : file;
        
        String compileCmd = config.getCompileCommand();
        String runCmd = config.getRunCommand();

        if (compileCmd != null) {
            compileCmd = compileCmd.replace("{file}", file).replace("{file_no_ext}", fileNoExt);
            // Fallback for hardcoded "Main.java" or "main.cpp"
            if (file.toLowerCase().endsWith(".java")) compileCmd = compileCmd.replace("Main.java", file);
            if (file.toLowerCase().endsWith(".cpp")) compileCmd = compileCmd.replace("main.cpp", file);
        }

        if (runCmd != null) {
            runCmd = runCmd.replace("{file}", file).replace("{file_no_ext}", fileNoExt);
            // Fallback for hardcoded "java Main" or "python main.py" or "./main"
            if (file.toLowerCase().endsWith(".java")) runCmd = runCmd.replace("Main", fileNoExt);
            if (file.toLowerCase().endsWith(".py")) runCmd = runCmd.replace("main.py", file);
            if (file.toLowerCase().endsWith(".cpp")) runCmd = runCmd.replace("main", fileNoExt);
            if (file.toLowerCase().endsWith(".js")) runCmd = runCmd.replace("main.js", file);
        }

        String shellCmd;
        if (config.isInterpreted()) {
            shellCmd = runCmd + " < input.txt";
        } else {
            shellCmd = compileCmd + " && " + runCmd + " < input.txt";
        }
        
        cmdList.add(shellCmd);
        return cmdList.toArray(new String[0]);
    }

    public List<SyntaxErrorDTO> lintCode(ExecutionRequest request) {
        List<SyntaxErrorDTO> errors = new ArrayList<>();
        String language = request.getLanguage().toLowerCase();
        LanguageConfig config = languageRegistry.getConfig(language).orElse(null);
        if (config == null || config.getCompileCommand() == null) return errors;

        try {
            Path tempDir = Files.createTempDirectory("codesync-lint-" + UUID.randomUUID());
            String fileName = config.getDefaultFileName() != null ? config.getDefaultFileName() : "main." + config.getExtension();
            Files.writeString(tempDir.resolve(fileName), request.getSourceCode());

            String workDir = "/app";
            String volume = tempDir.toAbsolutePath().toString() + ":" + workDir;
            
            String lintCmd = config.isInterpreted() ? 
                    config.getRunCommand().replace("{file}", fileName) : 
                    config.getCompileCommand().replace("{file}", fileName);

            String[] command = new String[]{"docker", "run", "--rm", "-v", volume, "-w", workDir, config.getDockerImage(), "sh", "-c", lintCmd};

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            StringBuilder stderr = new StringBuilder();
            try (BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errReader.readLine()) != null) stderr.append(line).append("\n");
            }

            process.waitFor(5, TimeUnit.SECONDS);
            process.destroyForcibly();

            org.springframework.util.FileSystemUtils.deleteRecursively(tempDir);

            if (stderr.length() > 0) {
                errors = parseSyntaxErrors(language, stderr.toString());
            }

        } catch (Exception e) {
            log.error("Lint execution error: {}", e.getMessage());
        }

        return errors;
    }

    private List<SyntaxErrorDTO> parseSyntaxErrors(String language, String stderr) {
        List<SyntaxErrorDTO> errors = new ArrayList<>();
        String[] lines = stderr.split("\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            
            int lineNumber = 1;
            String message = line;
            
            // Basic regex-based parsing for some languages
            Pattern pattern = Pattern.compile(".*:(\\d+):.*error: (.*)");
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                lineNumber = Integer.parseInt(matcher.group(1));
                message = matcher.group(2);
                errors.add(new SyntaxErrorDTO(lineNumber, 1, message, "error"));
            }
        }
        
        if (errors.isEmpty() && (stderr.toLowerCase().contains("error") || stderr.toLowerCase().contains("failed"))) {
            errors.add(new SyntaxErrorDTO(1, 1, "Compilation/Syntax Error", "error"));
        }

        return errors;
    }
}
