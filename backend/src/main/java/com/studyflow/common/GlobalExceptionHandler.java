package com.studyflow.common;

import com.studyflow.auth.InvalidCredentialsException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<ApiError> notFound(NotFoundException ex) {
    return respond(HttpStatus.NOT_FOUND, ApiError.of("NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiError> noResource(NoResourceFoundException ex) {
    return respond(HttpStatus.NOT_FOUND, ApiError.of("NOT_FOUND", "Resource not found"));
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  ResponseEntity<ApiError> invalidCredentials(InvalidCredentialsException ex) {
    return respond(HttpStatus.UNAUTHORIZED, ApiError.of("INVALID_CREDENTIALS", ex.getMessage()));
  }

  @ExceptionHandler(BusinessRuleException.class)
  ResponseEntity<ApiError> businessRule(BusinessRuleException ex) {
    return respond(HttpStatus.UNPROCESSABLE_ENTITY, ApiError.of(ex.code(), ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex) {
    Map<String, String> fields = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
    ex.getBindingResult()
        .getGlobalErrors()
        .forEach(error -> fields.putIfAbsent("_", error.getDefaultMessage()));
    return respond(
        HttpStatus.BAD_REQUEST,
        new ApiError("VALIDATION_FAILED", "The request has invalid fields", fields));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  ResponseEntity<ApiError> malformed(Exception ex) {
    return respond(
        HttpStatus.BAD_REQUEST, ApiError.of("MALFORMED_REQUEST", "The request could not be read"));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(Exception ex) {
    log.error("Unexpected error", ex);
    return respond(
        HttpStatus.INTERNAL_SERVER_ERROR, ApiError.of("INTERNAL_ERROR", "Unexpected server error"));
  }

  private static ResponseEntity<ApiError> respond(HttpStatus status, ApiError body) {
    return ResponseEntity.status(status).body(body);
  }
}
