package com.example.payheurebackend.service;

import com.example.payheurebackend.dto.PointageAnalyticsRequest;
import com.example.payheurebackend.dto.PointageAnalyticsResponse;

/**
 * Statistiques de complétude des pointages (sessions complètes/incomplètes et ratio), tous
 * salariés confondus, agrégées par jour, semaine ou mois.
 */
public interface PointageAnalyticsService {

    /**
     * Reconstitue les sessions de travail de tous les salariés actifs sur la période demandée et
     * les agrège par jour, semaine ou mois selon {@code request.granularite()}.
     *
     * @throws com.example.payheurebackend.exception.InvalidPeriodException si la période est incohérente
     */
    PointageAnalyticsResponse calculer(PointageAnalyticsRequest request);
}
