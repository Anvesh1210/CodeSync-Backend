package com.codesync.website.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException e, HttpServletRequest request) {
        log.error("Feign call failed for path {}: status={}, message={}", request.getRequestURI(), e.status(), e.getMessage());
        
        HttpStatus status = HttpStatus.resolve(e.status());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // Try to extract the error message from the JSON body of the FeignException
        String body = e.contentUTF8();
        String message = e.getMessage();
        
        if (body != null && !body.isBlank()) {
            try {
                // Regex to find "message":"..." regardless of spaces
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");
                java.util.regex.Matcher matcher = pattern.matcher(body);
                if (matcher.find()) {
                    message = matcher.group(1);
                }
            } catch (Exception ex) {
                // Fallback to default message
            }
        }

        return buildError(status, message, request.getRequestURI());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException e, HttpServletRequest request) {
        log.warn("Forbidden access: path={}, message={}", request.getRequestURI(), e.getMessage());
        return buildError(HttpStatus.FORBIDDEN, e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknownException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception for path {}: ", request.getRequestURI(), e);
        String message = e.getMessage() != null ? e.getMessage() : "An unexpected error occurred";
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, message, request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message, String path) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build();
        return ResponseEntity.status(status).body(errorResponse);
    }

    @Data
    @Builder
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
    }
}
