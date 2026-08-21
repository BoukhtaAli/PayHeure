package com.example.payheurebackend.service.impl;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.dto.PointageAnomalieJourResponse;
import com.example.payheurebackend.dto.PointageAnomalieRequest;
import com.example.payheurebackend.dto.PointageAnomalieResponse;
import com.example.payheurebackend.dto.PointageSessionResponse;
import com.example.payheurebackend.exception.InvalidPeriodException;
import com.example.payheurebackend.mapper.EmployeeMapper;
import com.example.payheurebackend.mapper.PointageMapper;
import com.example.payheurebackend.repository.PointageRepository;
import com.example.payheurebackend.service.PointageAnomalieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointageAnomalieServiceImpl implements PointageAnomalieService {

    private final PointageRepository pointageRepository;
    private final EmployeeMapper employeeMapper;
    private final PointageMapper pointageMapper;
    private final PointageSessionAssembler sessionAssembler;

    @Override
    public List<PointageAnomalieResponse> lister(PointageAnomalieRequest request) {
        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new InvalidPeriodException("La date de fin ne peut pas être antérieure à la date de début");
        }

        // Même raison qu'en calcul de paie (voir PaieServiceImpl.calculer) : on récupère les
        // badgeages de la/des journée(s) entière(s) couvertes par la période demandée, pas
        // seulement ceux strictement compris dedans, pour apparier correctement les sessions qui
        // chevauchent une des deux bornes.
        LocalDateTime journeeDebut = request.dateDebut().toLocalDate().atStartOfDay();
        LocalDateTime journeeFin = request.dateFin().toLocalDate().atTime(LocalTime.MAX);
        List<Pointage> pointages = pointageRepository.findActifsEntre(journeeDebut, journeeFin);

        // LinkedHashMap : préserve l'ordre de la requête (triée par salarié), pour un résultat
        // déterministe plutôt que dépendant de l'ordre de hachage.
        Map<Employee, List<Pointage>> byEmployee = new LinkedHashMap<>();
        for (Pointage pointage : pointages) {
            byEmployee.computeIfAbsent(pointage.getEmployee(), e -> new ArrayList<>()).add(pointage);
        }

        List<PointageAnomalieResponse> resultats = new ArrayList<>();
        for (Map.Entry<Employee, List<Pointage>> entry : byEmployee.entrySet()) {
            List<Pointage> pointagesEmploye = entry.getValue();
            List<PointageSessionResponse> sessions = sessionAssembler.construire(
                    pointagesEmploye, request.dateDebut(), request.dateFin());

            // Une journée par session en anomalie, avec l'ensemble des badgeages bruts de cette
            // journée (pas seulement le badgeage orphelin) : voir PointageAnomalieJourResponse.
            List<PointageAnomalieJourResponse> jours = sessions.stream()
                    .filter(PointageSessionResponse::anomalie)
                    .map(session -> new PointageAnomalieJourResponse(
                            session.date(),
                            pointageMapper.toResponseList(pointagesEmploye.stream()
                                    .filter(p -> p.getDateHeure().toLocalDate().equals(session.date()))
                                    .toList())))
                    .toList();

            if (!jours.isEmpty()) {
                resultats.add(new PointageAnomalieResponse(employeeMapper.toResponse(entry.getKey()), jours));
            }
        }
        return resultats;
    }
}
