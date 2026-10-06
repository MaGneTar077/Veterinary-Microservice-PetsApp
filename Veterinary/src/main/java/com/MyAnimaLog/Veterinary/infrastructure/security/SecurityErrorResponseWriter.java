package com.MyAnimaLog.Veterinary.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Writes the same {@code {timestamp, status, error, message}} JSON shape as
 * {@code GlobalExceptionHandler}, for the 401/403 cases that Spring Security resolves at the
 * filter level (before the request ever reaches a {@code @RestControllerAdvice}).
 */
final class SecurityErrorResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SecurityErrorResponseWriter() {
    }

    static void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        );
        OBJECT_MAPPER.writeValue(response.getOutputStream(), body);
    }
}
