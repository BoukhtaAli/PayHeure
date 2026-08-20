package com.example.payheurebackend.service;

import com.example.payheurebackend.dto.PointageCreateRequest;
import com.example.payheurebackend.dto.PointageResponse;

/** Enregistrement des pointages (badgeages bruts) d'un salarié. */
public interface PointageService {

    /**
     * Enregistre un nouveau badgeage pour le salarié désigné dans la requête.
     *
     * @throws com.example.payheurebackend.exception.ResourceNotFoundException si le salarié n'existe pas
     */
    PointageResponse creer(PointageCreateRequest request);
}
