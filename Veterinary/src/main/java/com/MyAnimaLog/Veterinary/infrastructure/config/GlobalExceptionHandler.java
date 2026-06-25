package com.MyAnimaLog.Veterinary.infrastructure.config;

import com.MyAnimaLog.Veterinary.domain.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        ));
    }

    @ExceptionHandler(InvalidVeterinaryNameException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidName(InvalidVeterinaryNameException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidVeterinaryEmailException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidEmail(InvalidVeterinaryEmailException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(VeterinaryAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyExists(VeterinaryAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(VeterinaryEmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailExists(VeterinaryEmailAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: " + ex.getMessage());
    }

    @ExceptionHandler(VeterinaryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(VeterinaryNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}