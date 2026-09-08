package com.example.demo.config;

import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.LazyInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Narrowly scoped to LazyInitializationException only - the exact bug class that
 * caused order cancellation to silently fail (an uninitialized Hibernate proxy
 * serialized to JSON after the transaction/session closed, with open-in-view=false).
 * Deliberately does NOT add a catch-all Exception handler: controllers already
 * catch their own exceptions and return structured error bodies, and Spring's
 * built-in handling of framework exceptions (e.g. MethodArgumentNotValidException)
 * already produces well-formed field-level error responses that a blanket handler
 * here would override.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(LazyInitializationException.class)
    public ResponseEntity<Map<String, String>> handleLazyInitialization(LazyInitializationException e, HttpServletRequest request) {
        log.error("Lazy-loaded association accessed outside its transaction on {} {}: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected server error occurred. Please try again."));
    }
}
