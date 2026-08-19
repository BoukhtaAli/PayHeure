package com.example.payheurebackend.dto;

import java.time.LocalDateTime;

/**
 * Critères de recherche d'un salarié. Chaque critère est facultatif ({@code null}/vide = non
 * filtrant) et se cumule avec les autres.
 *
 * @param query      texte recherché, comparé sans tenir compte de la casse au matricule, au nom
 *                   ET au prénom du salarié
 * @param dateDebut  avec {@code dateFin}, ne renvoie que les salariés ayant au moins un pointage
 *                   dans cette période ; ignoré si l'une des deux bornes est absente
 * @param dateFin    voir {@code dateDebut}
 */
public record EmployeeSearchCriteria(
        String query,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        Boolean deletedOnly
) {

    public EmployeeSearchCriteria(String query) {
        this(query, null, null, null);
    }

    public boolean restrictedToDeletedOnly() {
        return Boolean.TRUE.equals(deletedOnly);
    }

    public boolean hasPeriode() {
        return dateDebut != null && dateFin != null;
    }
}
