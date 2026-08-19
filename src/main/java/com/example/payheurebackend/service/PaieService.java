package com.example.payheurebackend.service;

import com.example.payheurebackend.dto.PaieCalculRequest;
import com.example.payheurebackend.dto.PaieCalculResponse;

/** Calcul de la paie d'un salarié à partir de ses pointages, sur une période donnée. */
public interface PaieService {

    /**
     * Récupère les pointages du salarié sur la période demandée, reconstitue les sessions de
     * travail (entrée/sortie) et calcule le montant dû au taux horaire fourni.
     *
     * @throws com.example.payheurebackend.exception.ResourceNotFoundException si le salarié n'existe pas
     * @throws com.example.payheurebackend.exception.InvalidPeriodException si la période est incohérente
     */
    PaieCalculResponse calculer(PaieCalculRequest request);
}
