package com.example.payheurebackend.dto;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorResponseTest {

    @Test
    void of_construitLaReponseAPartirDuStatutEtDuMessage() {
        Map<String, String> fieldErrors = Map.of("employeeId", "Le salarié est obligatoire");

        ApiErrorResponse response = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, "Les données envoyées sont invalides", fieldErrors);

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.error()).isEqualTo("Bad Request");
        assertThat(response.message()).isEqualTo("Les données envoyées sont invalides");
        assertThat(response.fieldErrors()).isEqualTo(fieldErrors);
        assertThat(response.timestamp()).isNotNull();
    }
}
