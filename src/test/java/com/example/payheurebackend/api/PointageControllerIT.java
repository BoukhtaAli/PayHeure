package com.example.payheurebackend.api;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.repository.EmployeeRepository;
import com.example.payheurebackend.repository.PointageRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bout en bout sur H2 : contrôleur, PointageServiceImpl / PointageAnomalieServiceImpl,
 * PointageMapper et GlobalExceptionHandler.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class PointageControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PointageRepository pointageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Employee saveEmployee() {
        return employeeRepository.save(Employee.builder().matricule("E001").nom("Boukhta").prenom("Amel").build());
    }

    private void punch(Employee employee, LocalDateTime dateHeure) {
        pointageRepository.save(Pointage.builder().employee(employee).dateHeure(dateHeure).build());
    }

    @Test
    void creer_salarieExistant_enregistreLeBadgeageEtRetourne201() throws Exception {
        Employee amel = saveEmployee();
        Map<String, Object> request = Map.of("employeeId", amel.getId(), "dateHeure", "2026-01-05T08:30:00");

        mockMvc.perform(post("/api/pointages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.dateHeure").value("2026-01-05T08:30:00"));

        assertThat(pointageRepository.existsByEmployeeId(amel.getId())).isTrue();
    }

    @Test
    void creer_salarieInexistant_retourne404SansRienEnregistrer() throws Exception {
        Map<String, Object> request = Map.of("employeeId", 999999, "dateHeure", "2026-01-05T08:30:00");

        mockMvc.perform(post("/api/pointages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Aucun salarié avec l'identifiant 999999"));

        assertThat(pointageRepository.count()).isZero();
    }

    @Test
    void creer_salarieSupprimeLogiquement_retourne404() throws Exception {
        Employee deleted = saveEmployee();
        deleted.markDeleted();
        employeeRepository.save(deleted);
        Map<String, Object> request = Map.of("employeeId", deleted.getId(), "dateHeure", "2026-01-05T08:30:00");

        mockMvc.perform(post("/api/pointages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void creer_champsObligatoiresManquants_retourne400() throws Exception {
        mockMvc.perform(post("/api/pointages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.employeeId").value("Le salarié est obligatoire"))
                .andExpect(jsonPath("$.fieldErrors.dateHeure").value("La date et l'heure sont obligatoires"));
    }

    @Test
    void creer_corpsMalforme_retourne400() throws Exception {
        mockMvc.perform(post("/api/pointages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ceci n'est pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le corps de la requête est absent ou mal formé"));
    }

    @Test
    void anomalies_badgeageSansSortie_signaleLeSalarie() throws Exception {
        Employee amel = saveEmployee();
        punch(amel, LocalDateTime.of(2026, 1, 5, 9, 0));

        Map<String, Object> request = Map.of(
                "dateDebut", "2026-01-05T00:00:00",
                "dateFin", "2026-01-05T23:59:59");

        mockMvc.perform(post("/api/pointages/anomalies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employee.matricule").value("E001"))
                .andExpect(jsonPath("$[0].anomalies.length()").value(1))
                .andExpect(jsonPath("$[0].anomalies[0].date").value("2026-01-05"))
                .andExpect(jsonPath("$[0].anomalies[0].pointages.length()").value(1))
                .andExpect(jsonPath("$[0].anomalies[0].pointages[0].dateHeure").value("2026-01-05T09:00:00"));
    }

    @Test
    void anomalies_aucunBadgeageIncomplet_retourneUneListeVide() throws Exception {
        Employee amel = saveEmployee();
        punch(amel, LocalDateTime.of(2026, 1, 5, 8, 0));
        punch(amel, LocalDateTime.of(2026, 1, 5, 12, 0));

        Map<String, Object> request = Map.of(
                "dateDebut", "2026-01-05T00:00:00",
                "dateFin", "2026-01-05T23:59:59");

        mockMvc.perform(post("/api/pointages/anomalies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void anomalies_periodeIncoherente_retourne400() throws Exception {
        Map<String, Object> request = Map.of(
                "dateDebut", "2026-01-31T00:00:00",
                "dateFin", "2026-01-01T00:00:00");

        mockMvc.perform(post("/api/pointages/anomalies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La date de fin ne peut pas être antérieure à la date de début"));
    }

    @Test
    void anomalies_champsObligatoiresManquants_retourne400() throws Exception {
        mockMvc.perform(post("/api/pointages/anomalies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.dateDebut").value("La date de début est obligatoire"))
                .andExpect(jsonPath("$.fieldErrors.dateFin").value("La date de fin est obligatoire"));
    }

    @Test
    void anomalies_salarieSupprimeLogiquement_estExclu() throws Exception {
        Employee deleted = saveEmployee();
        punch(deleted, LocalDateTime.of(2026, 1, 5, 9, 0));
        deleted.markDeleted();
        employeeRepository.save(deleted);

        Map<String, Object> request = Map.of(
                "dateDebut", "2026-01-05T00:00:00",
                "dateFin", "2026-01-05T23:59:59");

        mockMvc.perform(post("/api/pointages/anomalies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
