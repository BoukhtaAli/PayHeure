package com.example.payheurebackend.dto;

import java.util.List;

/**
 * Un salarié ayant au moins un pointage incomplet (badgeage sans sortie correspondante) sur la
 * période demandée.
 *
 * @param anomalies les seules journées en anomalie de ce salarié sur la période, jamais vide ;
 *                  les journées complètes (entrée + sortie) ne sont pas incluses ici, voir
 *                  {@code PaieController} pour consulter le détail complet d'un salarié
 */
public record PointageAnomalieResponse(
        EmployeeResponse employee,
        List<PointageAnomalieJourResponse> anomalies
) {
}
