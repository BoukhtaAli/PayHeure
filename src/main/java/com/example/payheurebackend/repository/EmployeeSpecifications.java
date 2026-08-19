package com.example.payheurebackend.repository;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.dto.EmployeeSearchCriteria;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Traduit les critères de recherche de salarié en prédicats JPA. */
public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    /** Combine en ET tous les critères réellement renseignés. */
    public static Specification<Employee> matching(EmployeeSearchCriteria criteria) {
        List<Specification<Employee>> specifications = new ArrayList<>();

        if (hasText(criteria.query())) {
            specifications.add(matriculeOrNameContains(criteria.query().trim()));
        }
        if (criteria.hasPeriode()) {
            specifications.add(workedBetween(criteria.dateDebut(), criteria.dateFin()));
        }
        specifications.add(criteria.restrictedToDeletedOnly() ? deletedOnly() : notDeleted());

        return Specification.allOf(specifications);
    }

    /**
     * Au moins un pointage dans la période. Sous-requête {@code EXISTS} plutôt qu'une jointure :
     * une jointure renverrait un salarié une fois par pointage correspondant dans la période,
     * cassant à la fois le "distinct" logique du résultat et la pagination.
     */
    private static Specification<Employee> workedBetween(LocalDateTime dateDebut, LocalDateTime dateFin) {
        return (root, query, builder) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Pointage> pointage = subquery.from(Pointage.class);
            subquery.select(pointage.get("id"))
                    .where(
                            builder.equal(pointage.get("employee"), root),
                            builder.between(pointage.get("dateHeure"), dateDebut, dateFin));
            return builder.exists(subquery);
        };
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
