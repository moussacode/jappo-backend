package sn.jappo.jappo_backend.config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException exception) {
        return build(HttpStatus.valueOf(exception.getStatusCode().value()), exception.getReason());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> erreurs = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(e -> erreurs.put(e.getField(), e.getDefaultMessage()));
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "Données invalides");
        body.put("erreurs", erreurs);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException exception) {
        return build(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception exception) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue est survenue.");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}