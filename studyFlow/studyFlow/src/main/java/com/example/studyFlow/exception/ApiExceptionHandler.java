package com.example.studyFlow.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), "NOT_FOUND", Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> conflict(ConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), "CONFLICT", Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, e -> e.getDefaultMessage() == null ? "Valor inválido." : e.getDefaultMessage(), (a, b) -> a));
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "Revise os campos informados.", "VALIDATION_ERROR", fields);
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class, MethodArgumentTypeMismatchException.class, HandlerMethodValidationException.class})
    ResponseEntity<ApiError> invalidParameter(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "Um dos valores informados é inválido.", "BAD_REQUEST", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "O corpo da requisição não é válido.", "BAD_REQUEST", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT, "A operação conflita com dados existentes.", "DATA_CONFLICT", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex) {
        log.error("Unhandled API exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno. Tente novamente.", "INTERNAL_ERROR", Map.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, String code, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message, code, fields, Instant.now()));
    }
}
