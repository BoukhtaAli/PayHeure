package com.example.payheurebackend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Requête de statistiques de complétude des pointages (nombre de sessions complètes/incomplètes
 * et ratio associé), tous salariés confondus, agrégées par jour, semaine ou mois selon
 * {@code granularite}. Même granularité de bornes que {@link PointageAnomalieRequest}.
 */
public record PointageAnalyticsRequest(

        @NotNull(message = "La date de début est obligatoire")
        LocalDateTime dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDateTime dateFin,

        @NotNull(message = "La granularité est obligatoire")
        Granularite granularite
) {
}
