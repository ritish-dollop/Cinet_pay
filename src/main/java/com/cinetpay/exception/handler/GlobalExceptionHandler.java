package com.cinetpay.exception.handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cinetpay.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error ->
                errors.put(((FieldError) error).getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Validation failed",
                "errors",  errors
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        log.error("Unhandled exception: {}", ex.getMessage());
        return ResponseEntity.internalServerError().body(ApiResponse.error(ex.getMessage()));
    }
}