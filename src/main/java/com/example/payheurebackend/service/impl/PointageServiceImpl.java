package com.example.payheurebackend.service.impl;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.dto.PointageCreateRequest;
import com.example.payheurebackend.dto.PointageResponse;
import com.example.payheurebackend.exception.ResourceNotFoundException;
import com.example.payheurebackend.mapper.PointageMapper;
import com.example.payheurebackend.repository.EmployeeRepository;
import com.example.payheurebackend.repository.PointageRepository;
import com.example.payheurebackend.service.PointageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PointageServiceImpl implements PointageService {

    private final EmployeeRepository employeeRepository;
    private final PointageRepository pointageRepository;
    private final PointageMapper pointageMapper;

    @Override
    public PointageResponse creer(PointageCreateRequest request) {
        // Un salarié supprimé logiquement n'est plus trouvable en recherche (voir
        // EmployeeServiceImpl) : lui refuser un nouveau pointage pour la même raison.
        Employee employee = employeeRepository.findById(request.employeeId())
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun salarié avec l'identifiant " + request.employeeId()));

        Pointage pointage = Pointage.builder()
                .employee(employee)
                .dateHeure(request.dateHeure())
                .build();

        return pointageMapper.toResponse(pointageRepository.save(pointage));
    }
}
