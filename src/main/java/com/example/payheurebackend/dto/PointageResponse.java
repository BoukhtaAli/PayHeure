package com.example.payheurebackend.dto;

import java.time.LocalDateTime;

/** Un badgeage brut, tel qu'enregistré en base (sans notion d'entrée/sortie). */
public record PointageResponse(
        Long id,
        LocalDateTime dateHeure
) {
}
