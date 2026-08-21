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
        when(pointageRepository.findByEmployeeIdAndDateHeureBetweenOrderByDateHeureAsc(1L, debut, fin)).thenReturn(pointages);

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
