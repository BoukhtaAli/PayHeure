package com.example.payheurebackend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Requête de création d'un pointage : un badgeage brut pour un salarié, sans notion
 * d'entrée/sortie (voir {@link com.example.payheurebackend.domain.Pointage}).
 */
public record PointageCreateRequest(

        @NotNull(message = "Le salarié est obligatoire")
        Long employeeId,

        @NotNull(message = "La date et l'heure sont obligatoires")
        LocalDateTime dateHeure
) {
}
