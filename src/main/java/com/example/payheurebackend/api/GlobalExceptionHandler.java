package com.example.payheurebackend.api;

import com.example.payheurebackend.dto.ApiErrorResponse;
import com.example.payheurebackend.exception.InvalidPeriodException;
import com.example.payheurebackend.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduit les exceptions de toutes les couches en réponses {@link ApiErrorResponse} homogènes,
 * afin que le front n'ait qu'un seul format d'erreur à interpréter.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Échec des contraintes de validation d'un payload. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), messageOf(error)));

        return build(HttpStatus.BAD_REQUEST, "Les données envoyées sont invalides", fieldErrors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    /** Période de calcul incohérente (date de fin avant la date de début). */
    @ExceptionHandler(InvalidPeriodException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPeriod(InvalidPeriodException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    /** Paramètre de requête non convertible : date non parsable, identifiant non numérique, etc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String message = "Le paramètre « %s » est invalide".formatted(exception.getName());
        return build(HttpStatus.BAD_REQUEST, message, Map.of());
    }

    /** Corps de requête absent ou JSON illisible. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return build(HttpStatus.BAD_REQUEST, "Le corps de la requête est absent ou mal formé", Map.of());
    }

    /** Violation de contrainte en base, par exemple un matricule déjà utilisé. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(DataIntegrityViolationException exception) {
        log.warn("Violation de contrainte d'intégrité", exception);
        return build(HttpStatus.CONFLICT, "La ressource existe déjà", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        log.error("Erreur inattendue", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue", Map.of());
    }

    private String messageOf(FieldError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valeur invalide";
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(status, message, fieldErrors));
    }
}
