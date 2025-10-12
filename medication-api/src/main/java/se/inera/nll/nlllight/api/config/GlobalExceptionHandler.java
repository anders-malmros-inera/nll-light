package se.inera.nll.nlllight.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception occurred: {}", ex.getMessage(), ex);
        
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        
        String message = ex.getMessage();
        HttpStatus status;
        String errorType;
        
        // Determine appropriate status based on message content and context
        if (message != null) {
            // Check if this is a "not found" error (resource lookup)
            if (message.contains("Prescription not found") || 
                message.contains("Prescriber not found") || 
                message.contains("Medication not found: ") ||  // With ID means direct lookup
                message.contains("Patient not found: ")) {     // With ID/userId means direct lookup
                status = HttpStatus.NOT_FOUND;
                errorType = "Not Found";
            }
            // Check if this is invalid input data (references in create/update requests)
            else if (message.contains("Patient not found") || message.contains("Medication not found")) {
                // Without specific context, treat as bad request (invalid reference in request)
                status = HttpStatus.BAD_REQUEST;
                errorType = "Bad Request";
            }
            // Authorization errors
            else if (message.contains("not authorized") || message.contains("Access denied")) {
                status = HttpStatus.FORBIDDEN;
                errorType = "Forbidden";
            }
            // Business logic errors
            else if (message.contains("Cannot") || message.contains("already")) {
                status = HttpStatus.BAD_REQUEST;
                errorType = "Bad Request";
            }
            // Default to 500
            else {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                errorType = "Internal Server Error";
            }
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorType = "Internal Server Error";
        }
        
        error.put("status", status.value());
        error.put("error", message != null ? message : errorType);  // Use message as error
        error.put("message", message);
        
        return ResponseEntity.status(status).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Validation Failed");
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError -> 
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage())
        );
        error.put("fieldErrors", fieldErrors);
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Invalid argument: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Bad Request");
        error.put("message", ex.getMessage());
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex) {
        logger.error("Security exception: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.FORBIDDEN.value());
        error.put("error", ex.getMessage());
        error.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
