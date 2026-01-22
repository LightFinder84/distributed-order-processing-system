package com.example.order_service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Automatically called by spring, explicit call is not allowed.
     *
     * @param ex Exception is throwed when the argument with @Valid is not valid by
     *           it's definition.
     * @return Http response object
     *
     * @see OrderController#placeOrder for argument types.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public final ResponseEntity<Map<String, String>> handleValidationExceptions(
            final MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * Automatically called by spring, explicit call is not allowed.
     *
     * @param ex Exception is throwed when a database entity is not found.
     * @return Http response object.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public final ResponseEntity<Map<String, String>> handleEntityNotFoundException(final EntityNotFoundException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", ex.getMessage());

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
}
