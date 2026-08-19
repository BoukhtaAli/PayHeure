package com.example.payheurebackend.dto;

/**
 * Représentation d'un salarié exposée par l'API.
 *
 * @param deleted {@code true} si le salarié a été supprimé logiquement
 */
public record EmployeeResponse(
        Long id,
        String matricule,
        String nom,
        String prenom,
        boolean deleted
) {
}
