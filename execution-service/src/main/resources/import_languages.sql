-- Truncate existing languages to only support the four specified languages
TRUNCATE TABLE supported_languages;

INSERT INTO supported_languages (name, extension, docker_image, compile_command, run_command, timeout_seconds, memory_limit, version_command, default_file_name, is_interpreted, boilerplate) VALUES
('java', 'java', 'eclipse-temurin:17-jdk-jammy', 'javac {file}', 'java {file_no_ext}', 15, '512m', 'java -version', 'Main.java', false, 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello World");\n    }\n}'),
('python', 'py', 'python:3.10-slim', NULL, 'python {file}', 10, '256m', 'python --version', 'main.py', true, 'print("Hello World")'),
('cpp', 'cpp', 'gcc:latest', 'g++ {file} -o {file_no_ext}', './{file_no_ext}', 10, '256m', 'g++ --version', 'main.cpp', false, '#include <iostream>\n\nint main() {\n    std::cout << "Hello World" << std::endl;\n    return 0;\n}'),
('javascript', 'js', 'node:18-slim', NULL, 'node {file}', 10, '256m', 'node -v', 'main.js', true, 'console.log("Hello World");');
