package com.example.payheurebackend.dto;

/**
 * Critères de recherche d'un salarié. Chaque critère est facultatif ({@code null}/vide = non
 * filtrant) et se cumule avec les autres.
 *
 * @param query texte recherché, comparé sans tenir compte de la casse au matricule, au nom
 *              ET au prénom du salarié
 */
public record EmployeeSearchCriteria(
        String query,
        Boolean deletedOnly
) {

    public EmployeeSearchCriteria(String query) {
        this(query, null);
    }

    public boolean restrictedToDeletedOnly() {
        return Boolean.TRUE.equals(deletedOnly);
    }
}
