package com.example.payheurebackend.service.impl;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.dto.PointageAnomalieRequest;
import com.example.payheurebackend.dto.PointageAnomalieResponse;
import com.example.payheurebackend.exception.InvalidPeriodException;
import com.example.payheurebackend.mapper.EmployeeMapperImpl;
import com.example.payheurebackend.mapper.PointageMapperImpl;
import com.example.payheurebackend.repository.PointageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointageAnomalieServiceImplTest {

    @Mock
    private PointageRepository pointageRepository;

    // Construit après l'injection des @Mock par MockitoExtension, voir EmployeeServiceImplTest.
    private PointageAnomalieServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PointageAnomalieServiceImpl(pointageRepository, new EmployeeMapperImpl(), new PointageMapperImpl(),
                new PointageSessionAssembler());
    }

    private static final Employee AMEL = Employee.builder().id(1L).matricule("E001").nom("Boukhta").prenom("Amel").build();
    private static final Employee YOUSSEF = Employee.builder().id(2L).matricule("E002").nom("El Amrani").prenom("Youssef").build();

    private static Pointage pointage(Employee employee, LocalDateTime dateHeure) {
        return Pointage.builder().employee(employee).dateHeure(dateHeure).build();
    }

    @Test
    void lister_periodeIncoherente_leveInvalidPeriodExceptionSansToucherAuxPointages() {
        LocalDateTime debut = LocalDateTime.of(2026, 1, 10, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 1, 0, 0);

        assertThatThrownBy(() -> service.lister(new PointageAnomalieRequest(debut, fin)))
                .isInstanceOf(InvalidPeriodException.class)
                .hasMessage("La date de fin ne peut pas être antérieure à la date de début");

        verifyNoInteractions(pointageRepository);
    }

    @Test
    void lister_aucunSalarieEnAnomalie_renvoieUneListeVide() {
        LocalDateTime debut = LocalDateTime.of(2026, 1, 5, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 5, 23, 59);
        List<Pointage> pointages = List.of(
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 8, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 12, 0)));
        when(pointageRepository.findActifsEntre(any(), any())).thenReturn(pointages);

        List<PointageAnomalieResponse> resultats = service.lister(new PointageAnomalieRequest(debut, fin));

        assertThat(resultats).isEmpty();
    }

    @Test
    void lister_badgeageSansSortie_signaleLeSalarieAvecSaSeuleAnomalie() {
        LocalDateTime debut = LocalDateTime.of(2026, 1, 5, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 5, 23, 59);
        // Amel a une session complète (non-anomalie, ne doit pas être remontée) et un badgeage
        // orphelin ; Youssef n'a que des sessions complètes et ne doit donc pas apparaître du tout.
        List<Pointage> pointages = List.of(
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 8, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 12, 0)),
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 14, 0)),
                pointage(YOUSSEF, LocalDateTime.of(2026, 1, 5, 9, 0)),
                pointage(YOUSSEF, LocalDateTime.of(2026, 1, 5, 17, 0)));
        when(pointageRepository.findActifsEntre(any(), any())).thenReturn(pointages);

        List<PointageAnomalieResponse> resultats = service.lister(new PointageAnomalieRequest(debut, fin));

        assertThat(resultats).hasSize(1);
        PointageAnomalieResponse resultat = resultats.get(0);
        assertThat(resultat.employee().matricule()).isEqualTo("E001");
        assertThat(resultat.anomalies()).hasSize(1);
        // La journée en anomalie liste tous les badgeages de la journée, pas seulement l'orphelin :
        // les deux du couple 8h/12h (déjà appariés en session complète) doivent y figurer aussi.
        assertThat(resultat.anomalies().get(0).date()).isEqualTo(LocalDateTime.of(2026, 1, 5, 0, 0).toLocalDate());
        assertThat(resultat.anomalies().get(0).pointages()).extracting(p -> p.dateHeure().toLocalTime().toString())
                .containsExactly("08:00", "12:00", "14:00");
    }

    @Test
    void lister_plusieursSalariesEnAnomalie_lesRenvoieTous() {
        LocalDateTime debut = LocalDateTime.of(2026, 1, 5, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 5, 23, 59);
        List<Pointage> pointages = List.of(
                pointage(AMEL, LocalDateTime.of(2026, 1, 5, 8, 0)),
                pointage(YOUSSEF, LocalDateTime.of(2026, 1, 5, 9, 0)));
        when(pointageRepository.findActifsEntre(any(), any())).thenReturn(pointages);

        List<PointageAnomalieResponse> resultats = service.lister(new PointageAnomalieRequest(debut, fin));

        assertThat(resultats).extracting(r -> r.employee().matricule()).containsExactly("E001", "E002");
    }
}
