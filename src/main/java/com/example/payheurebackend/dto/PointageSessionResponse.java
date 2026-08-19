package com.example.payheurebackend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Une session de travail reconstituée à partir de deux badgeages consécutifs d'une même
 * journée (entrée, puis sortie).
 *
 * @param heureSortie {@code null} si le badgeage d'entrée n'a pas de sortie correspondante
 *                    (nombre impair de pointages ce jour-là)
 * @param anomalie    {@code true} si {@code heureSortie} est {@code null} : cette session n'est
 *                    pas comptée dans le total travaillé, elle est juste signalée à l'écran
 */
public record PointageSessionResponse(
        LocalDate date,
        LocalDateTime heureEntree,
        LocalDateTime heureSortie,
        long dureeMinutes,
        boolean anomalie
) {
}
