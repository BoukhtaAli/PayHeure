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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaieServiceImpl implements PaieService {

    private final EmployeeRepository employeeRepository;
    private final PointageRepository pointageRepository;
    private final EmployeeMapper employeeMapper;
    private final PointageMapper pointageMapper;
    private final PointageSessionAssembler sessionAssembler;

    @Override
    public PaieCalculResponse calculer(PaieCalculRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun salarié avec l'identifiant " + request.employeeId()));

        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new InvalidPeriodException("La date de fin ne peut pas être antérieure à la date de début");
        }

        // On récupère les badgeages de la/des journée(s) entière(s) couvertes par la période
        // demandée, et pas seulement ceux strictement compris entre dateDebut et dateFin : une
        // session entrée/sortie peut chevaucher la fenêtre demandée sans que ses deux badgeages y
        // soient tous les deux inclus (ex. entrée à 9h, sortie à 12h, période filtrée 8h-10h). Les
        // apparier sur la journée complète évite de la signaler à tort comme anomalie à 0h ; seule
        // la portion qui chevauche réellement la fenêtre est ensuite comptée dans le total.
        LocalDateTime journeeDebut = request.dateDebut().toLocalDate().atStartOfDay();
        LocalDateTime journeeFin = request.dateFin().toLocalDate().atTime(LocalTime.MAX);
        List<Pointage> pointagesJournee = pointageRepository.findByEmployeeIdAndDateHeureBetweenOrderByDateHeureAsc(
                employee.getId(), journeeDebut, journeeFin);

        List<Pointage> pointagesPeriode = pointagesJournee.stream()
                .filter(p -> !p.getDateHeure().isBefore(request.dateDebut()) && !p.getDateHeure().isAfter(request.dateFin()))
                .toList();

        List<PointageSessionResponse> sessions = sessionAssembler.construire(pointagesJournee, request.dateDebut(), request.dateFin());

        long totalMinutes = sessions.stream()
                .filter(session -> !session.anomalie())
                .mapToLong(PointageSessionResponse::dureeMinutes)
                .sum();

        return new PaieCalculResponse(
                employeeMapper.toResponse(employee),
                request.dateDebut(),
                request.dateFin(),
                request.tauxHoraire(),
                pointageMapper.toResponseList(pointagesPeriode),
                sessions,
                totalMinutes,
                formatDuree(totalMinutes),
                montant(request.tauxHoraire(), totalMinutes),
                LocalDateTime.now());
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
