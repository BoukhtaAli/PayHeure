package com.example.payheurebackend.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Une journée en anomalie (badgeage sans sortie correspondante) pour un salarié.
 *
 * @param pointages tous les badgeages bruts enregistrés ce jour-là, pas seulement le badgeage
 *                  orphelin : le détail complet de la journée, affiché au clic côté écran, aide
 *                  à comprendre l'anomalie (ex. un seul badgeage oublié, ou une succession
 *                  incohérente de plusieurs badgeages)
 */
public record PointageAnomalieJourResponse(
        LocalDate date,
        List<PointageResponse> pointages
) {
}
