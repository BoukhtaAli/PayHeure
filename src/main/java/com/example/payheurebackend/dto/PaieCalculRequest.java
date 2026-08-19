package com.example.payheurebackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Requête de calcul de paie. Le taux horaire n'est jamais stocké côté serveur : il est saisi
 * par l'utilisateur à l'écran à chaque calcul, et ne vit que le temps de cet appel.
 * La période est bornée à l'heure et à la minute près (pas seulement au jour) : deux badgeages
 * du même jour peuvent ainsi être inclus ou exclus selon l'heure choisie.
 */
public record PaieCalculRequest(

        @NotNull(message = "Le salarié est obligatoire")
        Long employeeId,

        @NotNull(message = "La date de début est obligatoire")
        LocalDateTime dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDateTime dateFin,

        @NotNull(message = "Le taux horaire est obligatoire")
        @DecimalMin(value = "0.0", inclusive = false, message = "Le taux horaire doit être strictement positif")
        BigDecimal tauxHoraire
) {
}
