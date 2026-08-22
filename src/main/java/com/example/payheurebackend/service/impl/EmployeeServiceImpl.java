package com.example.payheurebackend.service.impl;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.dto.EmployeeResponse;
import com.example.payheurebackend.dto.EmployeeSearchCriteria;
import com.example.payheurebackend.dto.PageResponse;
import com.example.payheurebackend.exception.InvalidPeriodException;
import com.example.payheurebackend.exception.ResourceNotFoundException;
import com.example.payheurebackend.mapper.EmployeeMapper;
import com.example.payheurebackend.repository.EmployeeRepository;
import com.example.payheurebackend.repository.EmployeeSpecifications;
import com.example.payheurebackend.repository.PointageRepository;
import com.example.payheurebackend.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    /** Ordre d'affichage des résultats de recherche : par nom, puis prénom. */
    private static final Sort SEARCH_ORDER = Sort.by(Sort.Direction.ASC, "nom", "prenom");

    private final EmployeeRepository employeeRepository;
    private final PointageRepository pointageRepository;
    private final EmployeeMapper employeeMapper;
    private final PointageSessionAssembler sessionAssembler;

    @Override
    public PageResponse<EmployeeResponse> search(EmployeeSearchCriteria criteria, Pageable pageable) {
        if (criteria.hasPeriode() && criteria.dateFin().isBefore(criteria.dateDebut())) {
            throw new InvalidPeriodException("La date de fin ne peut pas être antérieure à la date de début");
        }

        Set<Long> idsPeriode = criteria.hasPeriode()
                ? employeeIdsAyantPointeDans(criteria.dateDebut(), criteria.dateFin())
                : Set.of();

        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), SEARCH_ORDER);
        Page<Employee> employees = employeeRepository.findAll(EmployeeSpecifications.matching(criteria, idsPeriode), sorted);
        return PageResponse.of(employees.map(employeeMapper::toResponse));
    }

    /**
     * Les identifiants des salariés ayant au moins une session (entrée/sortie ou badgeage
     * orphelin) chevauchant {@code [dateDebut, dateFin]}. Même logique que le calcul de paie et
     * les anomalies (voir {@code PaieServiceImpl.calculer} et {@code PointageSessionAssembler}) :
     * on récupère les badgeages de la/des journée(s) entière(s) couvertes par la période, pas
     * seulement ceux strictement compris dedans, pour apparier correctement une session qui
     * chevauche une des deux bornes sans que ses deux badgeages y soient tous les deux inclus
     * (ex. entrée à 8h, sortie à 11h, période filtrée 9h-11h). Sans ça, la liste de salariés
     * proposée à l'écran de calcul de paie pourrait omettre des salariés que le calcul lui-même
     * prendrait pourtant en compte.
     */
    private Set<Long> employeeIdsAyantPointeDans(LocalDateTime dateDebut, LocalDateTime dateFin) {
        LocalDateTime journeeDebut = dateDebut.toLocalDate().atStartOfDay();
        LocalDateTime journeeFin = dateFin.toLocalDate().atTime(LocalTime.MAX);
        List<Pointage> pointages = pointageRepository.findEntre(journeeDebut, journeeFin);

        // LinkedHashMap/LinkedHashSet : ordre déterministe, pas essentiel ici mais cohérent avec
        // PointageAnomalieServiceImpl qui suit la même logique.
        Map<Long, List<Pointage>> byEmployeeId = pointages.stream()
                .collect(Collectors.groupingBy(p -> p.getEmployee().getId(), LinkedHashMap::new, Collectors.toList()));

        Set<Long> ids = new LinkedHashSet<>();
        for (Map.Entry<Long, List<Pointage>> entry : byEmployeeId.entrySet()) {
            if (!sessionAssembler.construire(entry.getValue(), dateDebut, dateFin).isEmpty()) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    @Override
    public EmployeeResponse findById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Aucun salarié avec l'identifiant " + id));
        return employeeMapper.toResponse(employee);
    }
}
