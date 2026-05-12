package com.codesync.project.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.codesync.project.dto.response.ErrorResponse;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundException() {
        NotFoundException ex = new NotFoundException("Not Found");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test");
        
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Not Found");
    }

    @Test
    void handleBadRequestException() {
        BadRequestException ex = new BadRequestException("Bad Request");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Bad Request");
    }

    @Test
    void handleForbiddenException() {
        ForbiddenException ex = new ForbiddenException("Forbidden");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test");

        ResponseEntity<ErrorResponse> response = handler.handleForbidden(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("Forbidden");
    }

    @Test
    void handleConflictException() {
        ConflictException ex = new ConflictException("Conflict");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test");

        ResponseEntity<ErrorResponse> response = handler.handleConflict(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Conflict");
    }

    @Test
    void handleGeneralException() {
        Exception ex = new Exception("Error");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test");

        ResponseEntity<ErrorResponse> response = handler.handleUnknownException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Error");
    }

    @Test
    void handleValidationException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "message");
        HttpServletRequest request = mock(HttpServletRequest.class);
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("message");
    }
}
