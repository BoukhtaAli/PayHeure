package com.example.payheurebackend.repository;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.dto.EmployeeSearchCriteria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

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

    @Test
    void matching_sansCritere_excludLesSupprimes() {
        persistEmployee("E100", "Martin", "Alice");
        Employee deleted = persistEmployee("E200", "Dupont", "Bruno");
        deleted.markDeleted();
        entityManager.persistAndFlush(deleted);

        List<Employee> result = employeeRepository.findAll(EmployeeSpecifications.matching(new EmployeeSearchCriteria(null), List.of()));

        assertThat(result).extracting(Employee::getMatricule).containsExactly("E100");
    }

    @Test
    void matching_queryVide_neFiltrePasParTexte() {
        persistEmployee("E100", "Martin", "Alice");
        persistEmployee("E200", "Dupont", "Bruno");

        List<Employee> result = employeeRepository.findAll(EmployeeSpecifications.matching(new EmployeeSearchCriteria("   "), List.of()));

        assertThat(result).hasSize(2);
    }

    @Test
    void matching_query_trouveParMatriculeNomOuPrenomSansCasse() {
        persistEmployee("E100", "Martin", "Alice");
        persistEmployee("E200", "Dupont", "Bruno");

        assertThat(employeeRepository.findAll(EmployeeSpecifications.matching(new EmployeeSearchCriteria("e100"), List.of())))
                .extracting(Employee::getMatricule).containsExactly("E100");
        assertThat(employeeRepository.findAll(EmployeeSpecifications.matching(new EmployeeSearchCriteria("MARTIN"), List.of())))
                .extracting(Employee::getMatricule).containsExactly("E100");
        assertThat(employeeRepository.findAll(EmployeeSpecifications.matching(new EmployeeSearchCriteria("runo"), List.of())))
                .extracting(Employee::getMatricule).containsExactly("E200");
    }

    /**
     * Le calcul de "qui a pointé dans la période" est effectué en dehors de cette spécification
     * (voir {@code EmployeeServiceImpl.employeeIdsAyantPointeDans}) : elle se contente de filtrer
     * par identifiant. Voir {@code EmployeeControllerIT} pour un test bout en bout de la vraie
     * logique de chevauchement de session.
     */
    @Test
    void matching_periode_neRetourneQueLesSalariesDontLIdEstFourni() {
        Employee dedans = persistEmployee("E100", "Martin", "Alice");
        persistEmployee("E200", "Dupont", "Bruno");
        persistEmployee("E300", "Sans", "Pointage");

        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(
                null, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59), null);

        List<Employee> result = employeeRepository.findAll(EmployeeSpecifications.matching(criteria, List.of(dedans.getId())));

        assertThat(result).extracting(Employee::getMatricule).containsExactly("E100");
    }

    @Test
    void matching_deletedOnly_neRetourneQueLesSalariesSupprimes() {
        persistEmployee("E100", "Martin", "Alice");
        Employee deleted = persistEmployee("E200", "Dupont", "Bruno");
        deleted.markDeleted();
        entityManager.persistAndFlush(deleted);

        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(null, null, null, true);

        List<Employee> result = employeeRepository.findAll(EmployeeSpecifications.matching(criteria, List.of()));

        assertThat(result).extracting(Employee::getMatricule).containsExactly("E200");
    }

    @Test
    void matching_combineQueryEtPeriode_lesDeuxCriteresDoiventCorrespondre() {
        Employee correspond = persistEmployee("E100", "Martin", "Alice");
        persistEmployee("E101", "Martin", "Zoe");

        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(
                "martin", LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59), null);

        // Seul E100 a réellement pointé dans la période (id fourni) ; E101 correspond au texte
        // mais pas à la période, comme si son seul pointage tombait en dehors.
        List<Employee> result = employeeRepository.findAll(
                EmployeeSpecifications.matching(criteria, List.of(correspond.getId())));

        assertThat(result).extracting(Employee::getMatricule).containsExactly("E100");
    }
}
