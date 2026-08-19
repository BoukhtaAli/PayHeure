package com.example.payheurebackend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Salarié dont les pointages alimentent le calcul de paie. */
@Entity
@Table(name = "employee", uniqueConstraints = @UniqueConstraint(name = "uk_employee_matricule", columnNames = "matricule"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant métier du salarié, utilisé pour le rechercher. */
    @Column(nullable = false, length = 20)
    private String matricule;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    /**
     * Suppression logique : {@code null} tant que le salarié est actif. Renseignée, elle
     * exclut le salarié de la recherche sans perdre l'historique de ses pointages.
     */
    @Column
    private Instant deletedAt;

    /** Marque le salarié comme supprimé, sans effacer ses données ni ses pointages. */
    public void markDeleted() {
        this.deletedAt = Instant.now();
    }

    /** Annule une suppression logique : le salarié redevient trouvable. */
    public void restore() {
        this.deletedAt = null;
    }

    @Transient
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
