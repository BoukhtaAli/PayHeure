package com.example.payheurebackend.service.impl;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.dto.EmployeeResponse;
import com.example.payheurebackend.dto.EmployeeSearchCriteria;
import com.example.payheurebackend.dto.PageResponse;
import com.example.payheurebackend.exception.InvalidPeriodException;
import com.example.payheurebackend.exception.ResourceNotFoundException;
import com.example.payheurebackend.mapper.EmployeeMapper;
import com.example.payheurebackend.repository.EmployeeRepository;
import com.example.payheurebackend.repository.EmployeeSpecifications;
import com.example.payheurebackend.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    /** Ordre d'affichage des résultats de recherche : par nom, puis prénom. */
    private static final Sort SEARCH_ORDER = Sort.by(Sort.Direction.ASC, "nom", "prenom");

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    public PageResponse<EmployeeResponse> search(EmployeeSearchCriteria criteria, Pageable pageable) {
        if (criteria.hasPeriode() && criteria.dateFin().isBefore(criteria.dateDebut())) {
            throw new InvalidPeriodException("La date de fin ne peut pas être antérieure à la date de début");
        }

        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), SEARCH_ORDER);
        Page<Employee> employees = employeeRepository.findAll(EmployeeSpecifications.matching(criteria), sorted);
        return PageResponse.of(employees.map(employeeMapper::toResponse));
    }

    @Override
    public EmployeeResponse findById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Aucun salarié avec l'identifiant " + id));
        return employeeMapper.toResponse(employee);
    }
}
