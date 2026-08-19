package com.example.payheurebackend.service;

import com.example.payheurebackend.dto.EmployeeResponse;
import com.example.payheurebackend.dto.EmployeeSearchCriteria;
import com.example.payheurebackend.dto.PageResponse;
import org.springframework.data.domain.Pageable;

/** Recherche des salariés, préalable à un calcul de paie. */
public interface EmployeeService {

    /**
     * Les salariés correspondant à tous les critères fournis, une page à la fois.
     * Des critères vides renvoient l'intégralité des salariés actifs, paginée.
     */
    PageResponse<EmployeeResponse> search(EmployeeSearchCriteria criteria, Pageable pageable);

    /**
     * Un salarié par son identifiant.
     *
     * @throws com.example.payheurebackend.exception.ResourceNotFoundException s'il n'existe pas
     */
    EmployeeResponse findById(Long id);
}
