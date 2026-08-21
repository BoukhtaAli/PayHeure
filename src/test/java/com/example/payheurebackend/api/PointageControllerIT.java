package com.example.payheurebackend.api;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.repository.EmployeeRepository;
import com.example.payheurebackend.repository.PointageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Bout en bout sur H2 : contrôleur, PointageServiceImpl, PointageMapper et GlobalExceptionHandler. */
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
}
