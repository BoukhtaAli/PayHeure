package com.example.payheurebackend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Résultat des statistiques de complétude des pointages sur une période : une ligne par
 * sous-période (jour, semaine ou mois, voir {@code granularite}), plus les totaux sur l'ensemble
 * de la période demandée.
 *
 * @param periodes                 une par sous-période couverte par la période demandée, dans
 *                                  l'ordre chronologique ; seules celles ayant au moins une
 *                                  session (complète ou non) sont incluses
 * @param totalSessionsCompletes   somme de {@code sessionsCompletes} sur toutes les sous-périodes
 * @param totalSessionsIncompletes somme de {@code sessionsIncompletes} sur toutes les sous-périodes
 * @param ratioCompletionGlobal    pourcentage de sessions complètes sur l'ensemble de la période
 *                                 demandée (0 si aucune session), arrondi à 1 décimale
 */
public record PointageAnalyticsResponse(
        Granularite granularite,
        List<PointageAnalyticsPeriodeResponse> periodes,
        long totalSessionsCompletes,
        long totalSessionsIncompletes,
        BigDecimal ratioCompletionGlobal
) {
}
