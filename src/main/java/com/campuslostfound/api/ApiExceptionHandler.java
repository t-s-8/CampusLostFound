package com.campuslostfound.api;

import com.campuslostfound.exception.CampusLostFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getReason() == null ? "Request failed." : ex.getReason()));
    }

    @ExceptionHandler(CampusLostFoundException.class)
    public ResponseEntity<Map<String, String>> application(CampusLostFoundException ex) {
        String message = ex.getMessage();
        if (message == null || message.startsWith("Database error")) message = "The request could not be completed. Try again later.";
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage() == null ? "Invalid request." : ex.getMessage()));
    }
}
