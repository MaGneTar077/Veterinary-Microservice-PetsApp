package com.MyAnimaLog.Veterinary.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes the same {@code {timestamp, status, error, message, code}} JSON shape as
 * {@code GlobalExceptionHandler}, for the 401/403 cases that Spring Security resolves at the
 * filter level (before the request ever reaches a {@code @RestControllerAdvice}).
 */
final class SecurityErrorResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SecurityErrorResponseWriter() {
    }

    static void write(HttpServletResponse response, HttpStatus status, String message, String code) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("code", code);
        OBJECT_MAPPER.writeValue(response.getOutputStream(), body);
    }
}
