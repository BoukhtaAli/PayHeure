package com.example.payheurebackend.repository;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.dto.EmployeeSearchCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Traduit les critères de recherche de salarié en prédicats JPA. */
public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    /**
     * Combine en ET tous les critères réellement renseignés.
     *
     * @param idsAyantPointeDansPeriode identifiants des salariés ayant travaillé dans la période
     *                                  demandée (voir {@code EmployeeServiceImpl.employeeIdsAyantPointeDans}) ;
     *                                  ignoré si {@code criteria} n'a pas de période. Calculé en
     *                                  dehors de cette classe, car "avoir travaillé dans la
     *                                  période" exige de rejouer l'appariement entrée/sortie de
     *                                  {@code PointageSessionAssembler} (une session peut chevaucher
     *                                  une des deux bornes sans qu'aucun badgeage brut n'y soit
     *                                  strictement compris) — une simple comparaison SQL sur la
     *                                  date d'un badgeage ne suffit pas à le détecter.
     */
    public static Specification<Employee> matching(EmployeeSearchCriteria criteria, Collection<Long> idsAyantPointeDansPeriode) {
        List<Specification<Employee>> specifications = new ArrayList<>();

        if (hasText(criteria.query())) {
            specifications.add(matriculeOrNameContains(criteria.query().trim()));
        }
        if (criteria.hasPeriode()) {
            specifications.add(idIn(idsAyantPointeDansPeriode));
        }
        specifications.add(criteria.restrictedToDeletedOnly() ? deletedOnly() : notDeleted());

        return Specification.allOf(specifications);
    }

    private static Specification<Employee> idIn(Collection<Long> ids) {
        return (root, query, builder) -> root.get("id").in(ids);
    }

    /** Le texte recherché peut apparaître dans le matricule, le nom ou le prénom du salarié. */
    private static Specification<Employee> matriculeOrNameContains(String text) {
        String pattern = "%" + text.toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("matricule")), pattern),
                builder.like(builder.lower(root.get("nom")), pattern),
                builder.like(builder.lower(root.get("prenom")), pattern));
    }

    private static Specification<Employee> notDeleted() {
        return (root, query, builder) -> builder.isNull(root.get("deletedAt"));
    }

    private static Specification<Employee> deletedOnly() {
        return (root, query, builder) -> builder.isNotNull(root.get("deletedAt"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
