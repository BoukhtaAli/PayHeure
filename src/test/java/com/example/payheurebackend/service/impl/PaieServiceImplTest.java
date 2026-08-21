package com.example.payheurebackend.service.impl;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.dto.PaieCalculRequest;
import com.example.payheurebackend.dto.PaieCalculResponse;
import com.example.payheurebackend.dto.PointageSessionResponse;
import com.example.payheurebackend.exception.InvalidPeriodException;
import com.example.payheurebackend.exception.ResourceNotFoundException;
import com.example.payheurebackend.mapper.EmployeeMapperImpl;
import com.example.payheurebackend.mapper.PointageMapperImpl;
import com.example.payheurebackend.repository.EmployeeRepository;
import com.example.payheurebackend.repository.PointageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaieServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PointageRepository pointageRepository;

    // Construit après l'injection des @Mock par MockitoExtension, voir EmployeeServiceImplTest.
    private PaieServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaieServiceImpl(employeeRepository, pointageRepository, new EmployeeMapperImpl(), new PointageMapperImpl());
    }

    private static final Employee AMEL = Employee.builder().id(1L).matricule("E001").nom("Boukhta").prenom("Amel").build();

    private static Pointage pointage(Employee employee, LocalDateTime dateHeure) {
        return Pointage.builder().employee(employee).dateHeure(dateHeure).build();
    }

    @Test
    void calculer_salarieInexistant_leveResourceNotFoundExceptionSansToucherAuxPointages() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());
        PaieCalculRequest request = new PaieCalculRequest(1L, LocalDateTime.now(), LocalDateTime.now(), BigDecimal.TEN);

        assertThatThrownBy(() -> service.calculer(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Aucun salarié avec l'identifiant 1");

        verifyNoInteractions(pointageRepository);
    }

    @Test
    void calculer_salarieSupprimeLogiquement_leveResourceNotFoundException() {
        Employee deleted = Employee.builder().id(1L).matricule("E001").nom("Boukhta").prenom("Amel").build();
        deleted.markDeleted();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(deleted));
        PaieCalculRequest request = new PaieCalculRequest(1L, LocalDateTime.now(), LocalDateTime.now(), BigDecimal.TEN);

        assertThatThrownBy(() -> service.calculer(request)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void calculer_periodeIncoherente_leveInvalidPeriodException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(AMEL));
        LocalDateTime debut = LocalDateTime.of(2026, 1, 10, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 1, 0, 0);
        PaieCalculRequest request = new PaieCalculRequest(1L, debut, fin, BigDecimal.TEN);

        assertThatThrownBy(() -> service.calculer(request))
                .isInstanceOf(InvalidPeriodException.class)
                .hasMessage("La date de fin ne peut pas être antérieure à la date de début");

        verifyNoInteractions(pointageRepository);
    }

    @Test
    void calculer_journeeComplete_reconstitueUneSessionEtCalculeLeMontant() {
        LocalDateTime debut = LocalDateTime.of(2026, 1, 5, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 5, 23, 59);
        List<Pointage> pointages = List.of(
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 8, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 12, 0)));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(AMEL));
        when(pointageRepository.findByEmployeeIdAndDateHeureBetweenOrderByDateHeureAsc(
                anyLong(), any(), any())).thenReturn(pointages);

        PaieCalculResponse response = service.calculer(new PaieCalculRequest(1L, debut, fin, new BigDecimal("10.00")));

        assertThat(response.employee().matricule()).isEqualTo("E001");
        assertThat(response.pointages()).hasSize(2);
        assertThat(response.sessions()).hasSize(1);
        PointageSessionResponse session = response.sessions().get(0);
        assertThat(session.anomalie()).isFalse();
        assertThat(session.dureeMinutes()).isEqualTo(240);
        assertThat(response.totalMinutes()).isEqualTo(240);
        assertThat(response.totalDureeFormatee()).isEqualTo("4h 00min");
        assertThat(response.montantTotal()).isEqualByComparingTo("40.00");
        assertThat(response.dateHeureCalcul()).isNotNull();
    }

    @Test
    void calculer_badgeageSansSortie_estSignaleCommeAnomalieEtExcluDuTotal() {
        LocalDateTime debut = LocalDateTime.of(2026, 1, 5, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 5, 23, 59);
        List<Pointage> pointages = List.of(pointage(AMEL, LocalDateTime.of(2026, 1, 5, 9, 0)));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(AMEL));
        when(pointageRepository.findByEmployeeIdAndDateHeureBetweenOrderByDateHeureAsc(anyLong(), any(), any())).thenReturn(pointages);

        PaieCalculResponse response = service.calculer(new PaieCalculRequest(1L, debut, fin, new BigDecimal("10.00")));

        assertThat(response.sessions()).hasSize(1);
        PointageSessionResponse session = response.sessions().get(0);
        assertThat(session.anomalie()).isTrue();
        assertThat(session.heureSortie()).isNull();
        assertThat(session.dureeMinutes()).isZero();
        assertThat(response.totalMinutes()).isZero();
        assertThat(response.totalDureeFormatee()).isEqualTo("0h 00min");
        assertThat(response.montantTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void calculer_plusieursSessionsLeMemeJour_sontToutesReconstituees() {
        LocalDateTime debut = LocalDateTime.of(2026, 1, 5, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 5, 23, 59);
        List<Pointage> pointages = List.of(
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 8, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 12, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 13, 30)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 17, 30)));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(AMEL));
        when(pointageRepository.findByEmployeeIdAndDateHeureBetweenOrderByDateHeureAsc(anyLong(), any(), any())).thenReturn(pointages);

        PaieCalculResponse response = service.calculer(new PaieCalculRequest(1L, debut, fin, new BigDecimal("20.00")));

        assertThat(response.sessions()).hasSize(2);
        assertThat(response.sessions()).allMatch(s -> !s.anomalie());
        assertThat(response.totalMinutes()).isEqualTo(240 + 240);
        assertThat(response.totalDureeFormatee()).isEqualTo("8h 00min");
        assertThat(response.montantTotal()).isEqualByComparingTo("160.00");
    }

    @Test
    void calculer_fenetreCoupantUneSession_neCompteQueLaPortionChevauchante() {
        // Reproduit le bug signalé : filtre 8h-10h alors que le salarié a badgé entrée 9h / sortie
        // 12h. Le badgeage de sortie (12h) tombe hors de la fenêtre demandée mais doit quand même
        // être apparié avec l'entrée (9h) pour ne pas être signalé à tort comme anomalie à 0h ;
        // seule la portion 9h-10h qui chevauche la fenêtre doit être comptée dans le total.
        LocalDateTime fenetreDebut = LocalDateTime.of(2026, 8, 26, 8, 0);
        LocalDateTime fenetreFin = LocalDateTime.of(2026, 8, 26, 10, 0);
        List<Pointage> pointagesJournee = List.of(
                pointage(AMEL, LocalDateTime.of(2026, 8, 26, 9, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 8, 26, 12, 0)));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(AMEL));
        when(pointageRepository.findByEmployeeIdAndDateHeureBetweenOrderByDateHeureAsc(
                        1L,
                        LocalDateTime.of(2026, 8, 26, 0, 0),
                        LocalDateTime.of(2026, 8, 26, 23, 59, 59, 999_999_999)))
                .thenReturn(pointagesJournee);

        PaieCalculResponse response = service.calculer(
                new PaieCalculRequest(1L, fenetreDebut, fenetreFin, new BigDecimal("10.00")));

        assertThat(response.pointages()).hasSize(1); // seul le badgeage de 9h est dans la fenêtre demandée
        assertThat(response.sessions()).hasSize(1);
        PointageSessionResponse session = response.sessions().get(0);
        assertThat(session.anomalie()).isFalse();
        assertThat(session.heureEntree()).isEqualTo(LocalDateTime.of(2026, 8, 26, 9, 0));
        assertThat(session.heureSortie()).isEqualTo(LocalDateTime.of(2026, 8, 26, 10, 0));
        assertThat(session.dureeMinutes()).isEqualTo(60);
        assertThat(response.totalMinutes()).isEqualTo(60);
        assertThat(response.totalDureeFormatee()).isEqualTo("1h 00min");
        assertThat(response.montantTotal()).isEqualByComparingTo("10.00");
    }

    @Test
    void calculer_aucunPointageSurLaPeriode_totalZeroEtAucuneSession() {
        LocalDateTime debut = LocalDateTime.of(2026, 1, 5, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 5, 23, 59);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(AMEL));
        when(pointageRepository.findByEmployeeIdAndDateHeureBetweenOrderByDateHeureAsc(anyLong(), any(), any())).thenReturn(List.of());

        PaieCalculResponse response = service.calculer(new PaieCalculRequest(1L, debut, fin, new BigDecimal("20.00")));

        assertThat(response.sessions()).isEmpty();
        assertThat(response.pointages()).isEmpty();
        assertThat(response.totalMinutes()).isZero();
        assertThat(response.montantTotal()).isEqualByComparingTo("0.00");
    }
}
