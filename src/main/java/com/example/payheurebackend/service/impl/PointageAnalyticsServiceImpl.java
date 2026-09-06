package com.example.payheurebackend.service.impl;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.dto.Granularite;
import com.example.payheurebackend.dto.PointageAnalyticsPeriodeResponse;
import com.example.payheurebackend.dto.PointageAnalyticsRequest;
import com.example.payheurebackend.dto.PointageAnalyticsResponse;
import com.example.payheurebackend.dto.PointageSessionResponse;
import com.example.payheurebackend.exception.InvalidPeriodException;
import com.example.payheurebackend.repository.PointageRepository;
import com.example.payheurebackend.service.PointageAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Même reconstitution des sessions de travail que {@code PointageAnomalieServiceImpl} (via
 * {@link PointageSessionAssembler}, partagé pour détecter les anomalies exactement de la même
 * façon partout), mais agrégée par sous-période plutôt que restituée salarié par salarié.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointageAnalyticsServiceImpl implements PointageAnalyticsService {

    private final PointageRepository pointageRepository;
    private final PointageSessionAssembler sessionAssembler;

    /** Compteur de sessions complètes/incomplètes accumulé période par période dans {@code parPeriode}. */
    private record Compteur(long completes, long incompletes) {
        static Compteur of(boolean anomalie) {
            return anomalie ? new Compteur(0, 1) : new Compteur(1, 0);
        }

        Compteur plus(Compteur autre) {
            return new Compteur(completes + autre.completes(), incompletes + autre.incompletes());
        }
    }

    @Override
    public PointageAnalyticsResponse calculer(PointageAnalyticsRequest request) {
        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new InvalidPeriodException("La date de fin ne peut pas être antérieure à la date de début");
        }

        // Même raison qu'en recherche d'anomalies (voir PointageAnomalieServiceImpl) : les
        // badgeages sont récupérés sur la/les journée(s) entière(s) couvertes par la période
        // demandée, pas seulement ceux strictement compris dedans, pour apparier correctement les
        // sessions qui chevauchent une des deux bornes.
        LocalDateTime journeeDebut = request.dateDebut().toLocalDate().atStartOfDay();
        LocalDateTime journeeFin = request.dateFin().toLocalDate().atTime(LocalTime.MAX);
        List<Pointage> pointages = pointageRepository.findActifsEntre(journeeDebut, journeeFin);

        Map<Employee, List<Pointage>> byEmployee = new LinkedHashMap<>();
        for (Pointage pointage : pointages) {
            byEmployee.computeIfAbsent(pointage.getEmployee(), e -> new ArrayList<>()).add(pointage);
        }

        // TreeMap : les sous-périodes sont agrégées dans l'ordre chronologique, quel que soit
        // l'ordre de parcours des salariés/sessions ci-dessous.
        Map<LocalDate, Compteur> parPeriode = new TreeMap<>();
        for (List<Pointage> pointagesEmploye : byEmployee.values()) {
            List<PointageSessionResponse> sessions = sessionAssembler.construire(
                    pointagesEmploye, request.dateDebut(), request.dateFin());
            for (PointageSessionResponse session : sessions) {
                LocalDate cle = debutPeriode(session.date(), request.granularite());
                parPeriode.merge(cle, Compteur.of(session.anomalie()), Compteur::plus);
            }
        }

        List<PointageAnalyticsPeriodeResponse> periodes = parPeriode.entrySet().stream()
                .map(entry -> new PointageAnalyticsPeriodeResponse(
                        entry.getKey(), entry.getValue().completes(), entry.getValue().incompletes(),
                        ratio(entry.getValue().completes(), entry.getValue().incompletes())))
                .toList();

        long totalCompletes = periodes.stream().mapToLong(PointageAnalyticsPeriodeResponse::sessionsCompletes).sum();
        long totalIncompletes = periodes.stream().mapToLong(PointageAnalyticsPeriodeResponse::sessionsIncompletes).sum();

        return new PointageAnalyticsResponse(request.granularite(), periodes, totalCompletes, totalIncompletes,
                ratio(totalCompletes, totalIncompletes));
    }

    /** Premier jour de la sous-période : le jour lui-même, le lundi de sa semaine, ou le 1er de son mois. */
    private static LocalDate debutPeriode(LocalDate date, Granularite granularite) {
        return switch (granularite) {
            case JOUR -> date;
            case SEMAINE -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MOIS -> date.withDayOfMonth(1);
        };
    }

    /** Pourcentage de sessions complètes parmi l'ensemble, arrondi à 1 décimale ; 0 s'il n'y a aucune session. */
    private static BigDecimal ratio(long completes, long incompletes) {
        long total = completes + incompletes;
        if (total == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(completes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}
