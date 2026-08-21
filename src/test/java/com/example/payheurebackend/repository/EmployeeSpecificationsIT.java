package com.example.payheurebackend.repository;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.dto.EmployeeSearchCriteria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest exécute chaque prédicat contre une vraie base H2 (pas de mock de Root/CriteriaBuilder) :
 * seule façon fiable de vérifier une {@link org.springframework.data.jpa.domain.Specification},
 * dont la logique ne prend son sens qu'une fois traduite en SQL. Contexte transactionnel par
 * défaut (rollback après chaque test), comme les IT des contrôleurs.
 */
@DataJpaTest
class EmployeeSpecificationsIT {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Employee persistEmployee(String matricule, String nom, String prenom) {
        Employee employee = Employee.builder().matricule(matricule).nom(nom).prenom(prenom).build();
        return entityManager.persistAndFlush(employee);
    }

    private void persistPointage(Employee employee, LocalDateTime dateHeure) {
        entityManager.persistAndFlush(Pointage.builder().employee(employee).dateHeure(dateHeure).build());
    }

    @Test
    void matching_sansCritere_excludLesSupprimes() {
        persistEmployee("E100", "Martin", "Alice");
        Employee deleted = persistEmployee("E200", "Dupont", "Bruno");
        deleted.markDeleted();
        entityManager.persistAndFlush(deleted);

        List<Employee> result = employeeRepository.findAll(EmployeeSpecifications.matching(new EmployeeSearchCriteria(null)));

        assertThat(result).extracting(Employee::getMatricule).containsExactly("E100");
    }

    @Test
    void matching_queryVide_neFiltrePasParTexte() {
        persistEmployee("E100", "Martin", "Alice");
        persistEmployee("E200", "Dupont", "Bruno");

        List<Employee> result = employeeRepository.findAll(EmployeeSpecifications.matching(new EmployeeSearchCriteria("   ")));

        assertThat(result).hasSize(2);
    }

    @Test
    void matching_query_trouveParMatriculeNomOuPrenomSansCasse() {
        persistEmployee("E100", "Martin", "Alice");
        persistEmployee("E200", "Dupont", "Bruno");

        assertThat(employeeRepository.findAll(EmployeeSpecifications.matching(new EmployeeSearchCriteria("e100"))))
                .extracting(Employee::getMatricule).containsExactly("E100");
        assertThat(employeeRepository.findAll(EmployeeSpecifications.matching(new EmployeeSearchCriteria("MARTIN"))))
                .extracting(Employee::getMatricule).containsExactly("E100");
        assertThat(employeeRepository.findAll(EmployeeSpecifications.matching(new EmployeeSearchCriteria("runo"))))
                .extracting(Employee::getMatricule).containsExactly("E200");
    }

    @Test
    void matching_periode_neRetourneQueLesSalariesAyantPointeDedans() {
        Employee dedans = persistEmployee("E100", "Martin", "Alice");
        Employee dehors = persistEmployee("E200", "Dupont", "Bruno");
        persistEmployee("E300", "Sans", "Pointage");
        persistPointage(dedans, LocalDateTime.of(2026, 1, 5, 9, 0));
        persistPointage(dehors, LocalDateTime.of(2026, 2, 1, 9, 0));

        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(
                null, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59), null);

        List<Employee> result = employeeRepository.findAll(EmployeeSpecifications.matching(criteria));

        assertThat(result).extracting(Employee::getMatricule).containsExactly("E100");
    }

    @Test
    void matching_deletedOnly_neRetourneQueLesSalariesSupprimes() {
        persistEmployee("E100", "Martin", "Alice");
        Employee deleted = persistEmployee("E200", "Dupont", "Bruno");
        deleted.markDeleted();
        entityManager.persistAndFlush(deleted);

        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(null, null, null, true);

        List<Employee> result = employeeRepository.findAll(EmployeeSpecifications.matching(criteria));

        assertThat(result).extracting(Employee::getMatricule).containsExactly("E200");
    }

    @Test
    void matching_combineQueryEtPeriode_lesDeuxCriteresDoiventCorrespondre() {
        Employee correspond = persistEmployee("E100", "Martin", "Alice");
        Employee mauvaisePeriode = persistEmployee("E101", "Martin", "Zoe");
        persistPointage(correspond, LocalDateTime.of(2026, 1, 5, 9, 0));
        persistPointage(mauvaisePeriode, LocalDateTime.of(2026, 2, 1, 9, 0));

        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(
                "martin", LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59), null);

        List<Employee> result = employeeRepository.findAll(EmployeeSpecifications.matching(criteria));

        assertThat(result).extracting(Employee::getMatricule).containsExactly("E100");
    }
}
