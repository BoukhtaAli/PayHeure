package com.example.payheurebackend.service.impl;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.dto.PaieCalculRequest;
import com.example.payheurebackend.dto.PaieCalculResponse;
import com.example.payheurebackend.dto.PointageSessionResponse;
import com.example.payheurebackend.exception.InvalidPeriodException;
import com.example.payheurebackend.exception.ResourceNotFoundException;
import com.example.payheurebackend.mapper.EmployeeMapper;
import com.example.payheurebackend.mapper.PointageMapper;
import com.example.payheurebackend.repository.EmployeeRepository;
import com.example.payheurebackend.repository.PointageRepository;
import com.example.payheurebackend.service.PaieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaieServiceImpl implements PaieService {

    private final EmployeeRepository employeeRepository;
    private final PointageRepository pointageRepository;
    private final EmployeeMapper employeeMapper;
    private final PointageMapper pointageMapper;

    @Override
    public PaieCalculResponse calculer(PaieCalculRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun salarié avec l'identifiant " + request.employeeId()));

        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new InvalidPeriodException("La date de fin ne peut pas être antérieure à la date de début");
        }

        List<Pointage> pointages = pointageRepository.findByEmployeeIdAndDateHeureBetweenOrderByDateHeureAsc(
                employee.getId(), request.dateDebut(), request.dateFin());

        List<PointageSessionResponse> sessions = buildSessions(pointages);

        long totalMinutes = sessions.stream()
                .filter(session -> !session.anomalie())
                .mapToLong(PointageSessionResponse::dureeMinutes)
                .sum();

        return new PaieCalculResponse(
                employeeMapper.toResponse(employee),
                request.dateDebut(),
                request.dateFin(),
                request.tauxHoraire(),
                pointageMapper.toResponseList(pointages),
                sessions,
                totalMinutes,
                formatDuree(totalMinutes),
                montant(request.tauxHoraire(), totalMinutes),
                LocalDateTime.now());
    }

    /**
     * Apparie les badgeages jour par jour, dans l'ordre chronologique : le 1er badgeage d'une
     * journée est son entrée, le 2e sa sortie, le 3e une nouvelle entrée, etc. Un badgeage sans
     * sortie correspondante (nombre impair de badgeages ce jour-là, oubli de pointer par ex.)
     * est renvoyé comme anomalie et exclu du total travaillé.
     */
    private List<PointageSessionResponse> buildSessions(List<Pointage> pointages) {
        Map<LocalDate, List<Pointage>> byDay = pointages.stream()
                .collect(Collectors.groupingBy(p -> p.getDateHeure().toLocalDate(), LinkedHashMap::new, Collectors.toList()));

        List<PointageSessionResponse> sessions = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Pointage>> dayEntry : byDay.entrySet()) {
            List<Pointage> dayPointages = dayEntry.getValue();
            for (int i = 0; i < dayPointages.size(); i += 2) {
                LocalDateTime entree = dayPointages.get(i).getDateHeure();
                boolean hasSortie = i + 1 < dayPointages.size();
                LocalDateTime sortie = hasSortie ? dayPointages.get(i + 1).getDateHeure() : null;
                long dureeMinutes = hasSortie ? Duration.between(entree, sortie).toMinutes() : 0;

                sessions.add(new PointageSessionResponse(dayEntry.getKey(), entree, sortie, dureeMinutes, !hasSortie));
            }
        }
        return sessions;
    }

    /** {@code tauxHoraire * (totalMinutes / 60)}, arrondi à 2 décimales. */
    private BigDecimal montant(BigDecimal tauxHoraire, long totalMinutes) {
        BigDecimal heures = BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
        return tauxHoraire.multiply(heures).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatDuree(long totalMinutes) {
        return "%dh %02dmin".formatted(totalMinutes / 60, totalMinutes % 60);
    }
}
