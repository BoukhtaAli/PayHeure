package com.example.payheurebackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Agrégat des sessions de travail (voir {@link PointageSessionResponse}) d'une sous-période (jour,
 * semaine ou mois selon la granularité demandée), tous salariés confondus.
 *
 * @param dateDebut           premier jour de la sous-période : le jour lui-même, le lundi de sa
 *                            semaine, ou le 1er de son mois, selon la granularité demandée
 * @param sessionsCompletes   nombre de sessions entrée + sortie appariées sur la sous-période
 * @param sessionsIncompletes nombre de badgeages sans sortie correspondante sur la sous-période
 * @param ratioCompletion     pourcentage de sessions complètes parmi l'ensemble des sessions de la
 *                            sous-période (0 si elle n'en a aucune), arrondi à 1 décimale
 */
public record PointageAnalyticsPeriodeResponse(
        LocalDate dateDebut,
        long sessionsCompletes,
        long sessionsIncompletes,
        BigDecimal ratioCompletion
) {
}
