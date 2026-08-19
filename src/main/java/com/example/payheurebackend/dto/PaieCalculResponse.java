package com.example.payheurebackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Résultat du calcul de paie d'un salarié sur une période : ses pointages bruts, les sessions
 * de travail reconstituées à partir de ces pointages, et le montant dû.
 *
 * @param dateDebut          borne de début de la période demandée, à l'heure et à la minute près
 * @param dateFin            borne de fin de la période demandée, à l'heure et à la minute près
 * @param pointages          badgeages bruts de la période, dans l'ordre chronologique
 * @param sessions           sessions entrée/sortie reconstituées, une ou plusieurs par jour
 * @param totalMinutes       total des minutes travaillées, anomalies exclues
 * @param totalDureeFormatee total lisible, ex. {@code "12h 45min"}
 * @param montantTotal       {@code tauxHoraire * (totalMinutes / 60)}, arrondi à 2 décimales
 * @param dateHeureCalcul    date et heure auxquelles ce calcul a été effectué
 */
public record PaieCalculResponse(
        EmployeeResponse employee,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        BigDecimal tauxHoraire,
        List<PointageResponse> pointages,
        List<PointageSessionResponse> sessions,
        long totalMinutes,
        String totalDureeFormatee,
        BigDecimal montantTotal,
        LocalDateTime dateHeureCalcul
) {
}
