package com.sam.profilemanager.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");
        return Map.of("error", message);
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(UserNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleEmailConflict(EmailAlreadyInUseException ex) {
        return Map.of("error", ex.getMessage());
    }

    /** Гонка двух запросов с одним email: проверка existsByEmail прошла, но unique-констрейнт сработал. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleIntegrityViolation(DataIntegrityViolationException ex) {
        return Map.of("error", "Data conflict: email already in use");
    }
}
