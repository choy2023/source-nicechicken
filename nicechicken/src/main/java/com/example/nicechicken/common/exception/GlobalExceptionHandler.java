package com.example.nicechicken.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.stripe.exception.StripeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler (Applied to Modular Monolith)
 * Provides clean error responses that are easy for the frontend to parse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Exception handling for Bean Validation (@Valid) failures
     * @return 400 Bad Request in the form of { "fieldName": "error message" }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Handling business logic validation failures (IllegalArgumentException)
     * @return 400 Bad Request in the form of { "error": "message content" }
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handling data integrity violations (e.g., duplicate email signup, foreign key constraint violations, etc.)
     * @return 409 Conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Data collision occurred. (e.g., data already exists)");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handling Stripe API exceptions (e.g., card declined, invalid parameters, etc.)
     * @return 400 Bad Request with Stripe's user-friendly error message (if available)
     */
    @ExceptionHandler(StripeException.class)
    public ResponseEntity<Map<String, String>> handleStripeException(StripeException ex) {
        Map<String, String> response = new HashMap<>();
        String userMessage = ex.getUserMessage() != null ? ex.getUserMessage() : "An error occurred during payment processing.";
        response.put("error", "Payment failed: " + userMessage);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handling other unexpected server errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "An internal server error occurred. Please try again later.");
        
        // In a real production environment, hide the original message for security and only leave a log.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
