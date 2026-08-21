package com.example.payheurebackend.service;

import com.example.payheurebackend.dto.PointageAnomalieRequest;
import com.example.payheurebackend.dto.PointageAnomalieResponse;

import java.util.List;

/** Recherche, tous salariés confondus, des pointages incomplets ou oubliés sur une période. */
public interface PointageAnomalieService {

    /**
     * Reconstitue les sessions de travail de chaque salarié actif sur la période demandée et ne
     * renvoie que ceux ayant au moins un badgeage sans sortie correspondante.
     *
     * @throws com.example.payheurebackend.exception.InvalidPeriodException si la période est incohérente
     */
    List<PointageAnomalieResponse> lister(PointageAnomalieRequest request);
}
