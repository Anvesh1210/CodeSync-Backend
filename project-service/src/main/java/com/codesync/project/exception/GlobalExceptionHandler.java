package com.codesync.project.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.codesync.project.dto.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ErrorResponse> handleConflict(ConflictException exception, HttpServletRequest request) {
		return buildError(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception, HttpServletRequest request) {
		return buildError(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException exception, HttpServletRequest request) {
		return buildError(HttpStatus.FORBIDDEN, exception.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException exception, HttpServletRequest request) {
		return buildError(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
	public ResponseEntity<ErrorResponse> handleValidationException(Exception exception, HttpServletRequest request) {
		String message = "Validation failed";
		if (exception instanceof MethodArgumentNotValidException methodArgumentException
				&& methodArgumentException.getBindingResult().getFieldError() != null) {
			message = methodArgumentException.getBindingResult().getFieldError().getDefaultMessage();
		}
		if (exception instanceof BindException bindException && bindException.getBindingResult().getFieldError() != null) {
			message = bindException.getBindingResult().getFieldError().getDefaultMessage();
		}
		return buildError(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnknownException(Exception exception, HttpServletRequest request) {
		log.error("Unhandled exception for path {}", request.getRequestURI(), exception);
		String message = exception.getMessage() == null || exception.getMessage().isBlank()
				? "Internal server error"
				: exception.getMessage();
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
}
