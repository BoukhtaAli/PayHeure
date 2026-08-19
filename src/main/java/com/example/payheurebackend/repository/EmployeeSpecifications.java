package com.example.payheurebackend.repository;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.dto.EmployeeSearchCriteria;
import org.springframework.data.jpa.domain.Specification;

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
        specifications.add(criteria.restrictedToDeletedOnly() ? deletedOnly() : notDeleted());

        return Specification.allOf(specifications);
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
