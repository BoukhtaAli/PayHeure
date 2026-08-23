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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bout en bout sur H2 : contrôleur, PaieServiceImpl (reconstitution des sessions, calcul du
 * montant), les deux mappers et GlobalExceptionHandler.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class PaieControllerIT {

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
    void calculer_journeeStandard_calculeLeMontantDu() throws Exception {
        Employee amel = saveEmployee();
        punch(amel, LocalDateTime.of(2026, 1, 5, 8, 0));
        punch(amel, LocalDateTime.of(2026, 1, 5, 12, 0));

        Map<String, Object> request = Map.of(
                "employeeId", amel.getId(),
                "dateDebut", "2026-01-05T00:00:00",
                "dateFin", "2026-01-05T23:59:59",
                "tauxHoraire", 10.00);

        mockMvc.perform(post("/api/paie/calcul")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.matricule").value("E001"))
                .andExpect(jsonPath("$.pointages.length()").value(2))
                .andExpect(jsonPath("$.sessions.length()").value(1))
                .andExpect(jsonPath("$.sessions[0].anomalie").value(false))
                .andExpect(jsonPath("$.totalMinutes").value(240))
                .andExpect(jsonPath("$.totalDureeFormatee").value("4h 00min"))
                .andExpect(jsonPath("$.montantTotal").value(40.00));
    }

    @Test
    void calculer_sortieManquante_estSignaleeCommeAnomalie() throws Exception {
        Employee amel = saveEmployee();
        punch(amel, LocalDateTime.of(2026, 1, 5, 9, 0));

        Map<String, Object> request = Map.of(
                "employeeId", amel.getId(),
                "dateDebut", "2026-01-05T00:00:00",
                "dateFin", "2026-01-05T23:59:59",
                "tauxHoraire", 10.00);

        mockMvc.perform(post("/api/paie/calcul")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions[0].anomalie").value(true))
                .andExpect(jsonPath("$.totalMinutes").value(0))
                .andExpect(jsonPath("$.montantTotal").value(0.00));
    }

    @Test
    void calculer_salarieInexistant_retourne404() throws Exception {
        Map<String, Object> request = Map.of(
                "employeeId", 999999,
                "dateDebut", "2026-01-05T00:00:00",
                "dateFin", "2026-01-05T23:59:59",
                "tauxHoraire", 10.00);

        mockMvc.perform(post("/api/paie/calcul")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Aucun salarié avec l'identifiant 999999"));
    }

    @Test
    void calculer_periodeIncoherente_retourne400() throws Exception {
        Employee amel = saveEmployee();
        Map<String, Object> request = Map.of(
                "employeeId", amel.getId(),
                "dateDebut", "2026-01-31T00:00:00",
                "dateFin", "2026-01-01T00:00:00",
                "tauxHoraire", 10.00);

        mockMvc.perform(post("/api/paie/calcul")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La date de fin ne peut pas être antérieure à la date de début"));
    }

    @Test
    void calculer_champsObligatoiresManquants_retourne400AvecUneErreurParChamp() throws Exception {
        mockMvc.perform(post("/api/paie/calcul")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.employeeId").value("Le salarié est obligatoire"))
                .andExpect(jsonPath("$.fieldErrors.dateDebut").value("La date de début est obligatoire"))
                .andExpect(jsonPath("$.fieldErrors.dateFin").value("La date de fin est obligatoire"))
                .andExpect(jsonPath("$.fieldErrors.tauxHoraire").value("Le taux horaire est obligatoire"));
    }

    @Test
    void calculer_tauxHoraireNulOuNegatif_retourne400() throws Exception {
        Employee amel = saveEmployee();
        Map<String, Object> request = Map.of(
                "employeeId", amel.getId(),
                "dateDebut", "2026-01-05T00:00:00",
                "dateFin", "2026-01-05T23:59:59",
                "tauxHoraire", 0);

        mockMvc.perform(post("/api/paie/calcul")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.tauxHoraire").value("Le taux horaire doit être strictement positif"));
    }

    @Test
    void calculer_corpsMalforme_retourne400() throws Exception {
        mockMvc.perform(post("/api/paie/calcul")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ceci n'est pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le corps de la requête est absent ou mal formé"));
    }
}
