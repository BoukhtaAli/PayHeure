package com.example.payheurebackend.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeSearchCriteriaTest {

    @Test
    void constructeurUnSeulArgument_nInitialiseQueLaQuery() {
        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria("amel");

        assertThat(criteria.query()).isEqualTo("amel");
        assertThat(criteria.dateDebut()).isNull();
        assertThat(criteria.dateFin()).isNull();
        assertThat(criteria.deletedOnly()).isNull();
    }

    @Test
    void hasPeriode_vraiSeulementSiLesDeuxBornesSontRenseignees() {
        LocalDateTime now = LocalDateTime.now();

        assertThat(new EmployeeSearchCriteria(null, now, now, null).hasPeriode()).isTrue();
        assertThat(new EmployeeSearchCriteria(null, now, null, null).hasPeriode()).isFalse();
        assertThat(new EmployeeSearchCriteria(null, null, now, null).hasPeriode()).isFalse();
        assertThat(new EmployeeSearchCriteria(null, null, null, null).hasPeriode()).isFalse();
    }

    @Test
    void restrictedToDeletedOnly_vraiSeulementSiExplicitementTrue() {
        assertThat(new EmployeeSearchCriteria(null, null, null, true).restrictedToDeletedOnly()).isTrue();
        assertThat(new EmployeeSearchCriteria(null, null, null, false).restrictedToDeletedOnly()).isFalse();
        assertThat(new EmployeeSearchCriteria(null, null, null, null).restrictedToDeletedOnly()).isFalse();
    }
}
