package com.example.payheurebackend.api;

import com.example.payheurebackend.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les branches "prévisibles" (validation, salarié introuvable, période invalide, paramètre non
 * convertible, corps illisible) sont exercées de bout en bout par les tests d'intégration des
 * contrôleurs, dans des conditions plus réalistes qu'un appel direct ici. Seules les deux
 * branches qu'aucun endpoint actuel ne peut déclencher naturellement (violation de contrainte
 * bdd, erreur générique inattendue) sont testées directement.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleConflict_retourne409AvecMessageGenerique() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleConflict(new DataIntegrityViolationException("uk_employee_matricule"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("La ressource existe déjà");
        assertThat(response.getBody().fieldErrors()).isEmpty();
    }

    @Test
    void handleUnexpected_retourne500AvecMessageGenerique() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Une erreur interne est survenue");
        assertThat(response.getBody().fieldErrors()).isEmpty();
    }
}
