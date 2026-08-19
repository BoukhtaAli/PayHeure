package com.example.payheurebackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

/** Corps de réponse unique pour toutes les erreurs de l'API. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        /** Erreurs de validation, indexées par nom de champ. Absent si l'erreur n'est pas une validation. */
        Map<String, String> fieldErrors
) {

    public static ApiErrorResponse of(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
    }
}
