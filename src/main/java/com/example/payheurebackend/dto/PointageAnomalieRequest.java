package com.example.payheurebackend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Requête de recherche des pointages incomplets (badgeage sans sortie correspondante) ou
 * carrément oubliés, tous salariés confondus, sur une période. Même granularité que
 * {@link PaieCalculRequest} : bornée à l'heure et à la minute près, pas seulement au jour.
 */
public record PointageAnomalieRequest(

        @NotNull(message = "La date de début est obligatoire")
        LocalDateTime dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDateTime dateFin
) {
}
