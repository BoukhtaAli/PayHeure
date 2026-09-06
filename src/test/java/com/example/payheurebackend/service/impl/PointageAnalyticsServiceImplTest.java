package com.example.payheurebackend.service.impl;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.dto.Granularite;
import com.example.payheurebackend.dto.PointageAnalyticsRequest;
import com.example.payheurebackend.dto.PointageAnalyticsResponse;
import com.example.payheurebackend.exception.InvalidPeriodException;
import com.example.payheurebackend.repository.PointageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointageAnalyticsServiceImplTest {

    @Mock
    private PointageRepository pointageRepository;

    // Construit après l'injection des @Mock par MockitoExtension, voir EmployeeServiceImplTest.
    private PointageAnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PointageAnalyticsServiceImpl(pointageRepository, new PointageSessionAssembler());
    }

    private static final Employee AMEL = Employee.builder().id(1L).matricule("E001").nom("Boukhta").prenom("Amel").build();
    private static final Employee YOUSSEF = Employee.builder().id(2L).matricule("E002").nom("El Amrani").prenom("Youssef").build();

    private static Pointage pointage(Employee employee, LocalDateTime dateHeure) {
        return Pointage.builder().employee(employee).dateHeure(dateHeure).build();
    }

    @Test
    void calculer_periodeIncoherente_leveInvalidPeriodExceptionSansToucherAuxPointages() {
        LocalDateTime debut = LocalDateTime.of(2026, 1, 10, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 1, 0, 0);

        assertThatThrownBy(() -> service.calculer(new PointageAnalyticsRequest(debut, fin, Granularite.JOUR)))
                .isInstanceOf(InvalidPeriodException.class)
                .hasMessage("La date de fin ne peut pas être antérieure à la date de début");

        verifyNoInteractions(pointageRepository);
    }

    @Test
    void calculer_aucunPointage_renvoieDesTotauxAZeroSansPeriode() {
        LocalDateTime debut = LocalDateTime.of(2026, 1, 5, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 5, 23, 59);
        when(pointageRepository.findActifsEntre(any(), any())).thenReturn(List.of());

        PointageAnalyticsResponse resultat = service.calculer(new PointageAnalyticsRequest(debut, fin, Granularite.JOUR));

        assertThat(resultat.periodes()).isEmpty();
        assertThat(resultat.totalSessionsCompletes()).isZero();
        assertThat(resultat.totalSessionsIncompletes()).isZero();
        assertThat(resultat.ratioCompletionGlobal()).isEqualByComparingTo("0.0");
    }

    @Test
    void calculer_granulariteJour_agregeParJourTousSalariesConfondus() {
        LocalDateTime debut = LocalDateTime.of(2026, 1, 5, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 6, 23, 59);
        // 5 janvier : une session complète (Amel) et un badgeage orphelin (Youssef) -> 1
        // complète, 1 incomplète, ratio 50%. 6 janvier : une seule session complète -> 100%.
        List<Pointage> pointages = List.of(
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 8, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 12, 0)),
                pointage(YOUSSEF, LocalDateTime.of(2026, 1, 5, 9, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 6, 8, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 6, 12, 0)));
        when(pointageRepository.findActifsEntre(any(), any())).thenReturn(pointages);

        PointageAnalyticsResponse resultat = service.calculer(new PointageAnalyticsRequest(debut, fin, Granularite.JOUR));

        assertThat(resultat.periodes()).hasSize(2);
        assertThat(resultat.periodes().get(0).dateDebut()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(resultat.periodes().get(0).sessionsCompletes()).isEqualTo(1);
        assertThat(resultat.periodes().get(0).sessionsIncompletes()).isEqualTo(1);
        assertThat(resultat.periodes().get(0).ratioCompletion()).isEqualByComparingTo("50.0");
        assertThat(resultat.periodes().get(1).dateDebut()).isEqualTo(LocalDate.of(2026, 1, 6));
        assertThat(resultat.periodes().get(1).sessionsCompletes()).isEqualTo(1);
        assertThat(resultat.periodes().get(1).sessionsIncompletes()).isZero();
        assertThat(resultat.periodes().get(1).ratioCompletion()).isEqualByComparingTo("100.0");
        assertThat(resultat.totalSessionsCompletes()).isEqualTo(2);
        assertThat(resultat.totalSessionsIncompletes()).isEqualTo(1);
        // 2 complètes sur 3 sessions au total (200/3 = 66,66... arrondi à 1 décimale).
        assertThat(resultat.ratioCompletionGlobal()).isEqualByComparingTo("66.7");
    }

    @Test
    void calculer_granulariteSemaine_regroupeLesJoursDeLaMemeSemaineIso() {
        // Lundi 5 janvier 2026 et mercredi 7 janvier 2026 sont dans la même semaine ISO.
        LocalDateTime debut = LocalDateTime.of(2026, 1, 5, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 7, 23, 59);
        List<Pointage> pointages = List.of(
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 8, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 12, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 7, 8, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 7, 12, 0)));
        when(pointageRepository.findActifsEntre(any(), any())).thenReturn(pointages);

        PointageAnalyticsResponse resultat = service.calculer(new PointageAnalyticsRequest(debut, fin, Granularite.SEMAINE));

        assertThat(resultat.periodes()).hasSize(1);
        assertThat(resultat.periodes().get(0).dateDebut()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(resultat.periodes().get(0).sessionsCompletes()).isEqualTo(2);
    }

    @Test
    void calculer_granulariteMois_regroupeLesJoursDuMemeMois() {
        LocalDateTime debut = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 31, 23, 59);
        List<Pointage> pointages = List.of(
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 8, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 12, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 20, 8, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 20, 12, 0)));
        when(pointageRepository.findActifsEntre(any(), any())).thenReturn(pointages);

        PointageAnalyticsResponse resultat = service.calculer(new PointageAnalyticsRequest(debut, fin, Granularite.MOIS));

        assertThat(resultat.periodes()).hasSize(1);
        assertThat(resultat.periodes().get(0).dateDebut()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(resultat.periodes().get(0).sessionsCompletes()).isEqualTo(2);
    }
}
