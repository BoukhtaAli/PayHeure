package com.example.payheurebackend.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seuls {@code markDeleted}/{@code restore}/{@code isDeleted} sont du code écrit à la main sur
 * cette entité (le reste vient de Lombok, exclu de la couverture, voir lombok.config).
 */
class EmployeeTest {

    @Test
    void unSalarieFraichementCree_nestPasSupprime() {
        Employee employee = Employee.builder().matricule("E001").nom("Boukhta").prenom("Amel").build();

        assertThat(employee.isDeleted()).isFalse();
        assertThat(employee.getDeletedAt()).isNull();
    }

    @Test
    void markDeleted_renseigneDeletedAtEtBasculeIsDeleted() {
        Employee employee = Employee.builder().matricule("E001").nom("Boukhta").prenom("Amel").build();

        employee.markDeleted();

        assertThat(employee.isDeleted()).isTrue();
        assertThat(employee.getDeletedAt()).isNotNull();
    }

    @Test
    void restore_effaceDeletedAtEtRedevientActif() {
        Employee employee = Employee.builder().matricule("E001").nom("Boukhta").prenom("Amel").build();
        employee.markDeleted();

        employee.restore();

        assertThat(employee.isDeleted()).isFalse();
        assertThat(employee.getDeletedAt()).isNull();
    }
}
