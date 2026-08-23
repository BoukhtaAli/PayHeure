package com.example.payheurebackend.api;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.repository.EmployeeRepository;
import com.example.payheurebackend.repository.PointageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bout en bout sur une base H2 en mémoire (voir src/test/resources/application.yaml) : exerce
 * ensemble le contrôleur, EmployeeServiceImpl, EmployeeSpecifications, EmployeeMapper et
 * GlobalExceptionHandler. Chaque test tourne dans sa propre transaction, annulée à la fin (voir
 * @Transactional) : aucune donnée ne fuit d'un test à l'autre malgré le contexte Spring partagé.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class EmployeeControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PointageRepository pointageRepository;

    private Employee save(String matricule, String nom, String prenom) {
        return employeeRepository.save(Employee.builder().matricule(matricule).nom(nom).prenom(prenom).build());
    }

    private void punch(Employee employee, LocalDateTime dateHeure) {
        pointageRepository.save(Pointage.builder().employee(employee).dateHeure(dateHeure).build());
    }

    @Test
    void search_sansCritere_excludLesSalariesSupprimes() throws Exception {
        save("E100", "Martin", "Alice");
        Employee deleted = save("E200", "Dupont", "Bruno");
        deleted.markDeleted();
        employeeRepository.save(deleted);

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].matricule").value("E100"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void search_avecQuery_filtreParMatriculeNomOuPrenomSansCasse() throws Exception {
        save("E100", "Martin", "Alice");
        save("E200", "Dupont", "Bruno");
        save("E300", "Martin", "Chloe");

        mockMvc.perform(get("/api/employees").param("query", "martin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                // Trié par nom puis prénom : Alice avant Chloe (même nom "Martin").
                .andExpect(jsonPath("$.content[0].prenom").value("Alice"))
                .andExpect(jsonPath("$.content[1].prenom").value("Chloe"));

        mockMvc.perform(get("/api/employees").param("query", "e200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].nom").value("Dupont"));
    }

    @Test
    void search_avecPeriode_neRenvoieQueLesSalariesAyantPointeDansLaPeriode() throws Exception {
        Employee dedans = save("E100", "Martin", "Alice");
        Employee dehors = save("E200", "Dupont", "Bruno");
        save("E300", "Sans", "Pointage");
        punch(dedans, LocalDateTime.of(2026, 1, 5, 9, 0));
        punch(dehors, LocalDateTime.of(2026, 2, 1, 9, 0));

        mockMvc.perform(get("/api/employees")
                        .param("dateDebut", "2026-01-01T00:00:00")
                        .param("dateFin", "2026-01-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].matricule").value("E100"));
    }

    /**
     * Reproduit le bug signalé : un salarié pointé de 8h à 11h doit apparaître même pour une
     * période filtrée plus étroite que sa session (9h-11h), pas seulement pour une période qui
     * englobe ses deux badgeages bruts (8h-12h). Voir EmployeeServiceImpl.employeeIdsAyantPointeDans.
     */
    @Test
    void search_avecPeriode_trouveUnSalarieDontLaSessionChevaucheSansContenirLesDeuxBadgeages() throws Exception {
        Employee employee = save("E004", "Martin", "Alice");
        punch(employee, LocalDateTime.of(2026, 8, 19, 8, 0));
        punch(employee, LocalDateTime.of(2026, 8, 19, 11, 0));

        mockMvc.perform(get("/api/employees")
                        .param("dateDebut", "2026-08-19T09:00:00")
                        .param("dateFin", "2026-08-19T11:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].matricule").value("E004"));
    }

    @Test
    void search_periodeIncoherente_retourne400() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .param("dateDebut", "2026-01-31T00:00:00")
                        .param("dateFin", "2026-01-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La date de fin ne peut pas être antérieure à la date de début"));
    }

    @Test
    void search_taillePageEstBorneeEntreUnEtCent() throws Exception {
        save("E100", "Martin", "Alice");
        save("E200", "Dupont", "Bruno");
        save("E300", "Martin", "Chloe");

        mockMvc.perform(get("/api/employees").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/employees").param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1));

        mockMvc.perform(get("/api/employees").param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void findById_salarieExistant_retourneSesInformations() throws Exception {
        Employee amel = save("E001", "Boukhta", "Amel");

        mockMvc.perform(get("/api/employees/" + amel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricule").value("E001"))
                .andExpect(jsonPath("$.nom").value("Boukhta"))
                .andExpect(jsonPath("$.prenom").value("Amel"))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    void findById_salarieInexistant_retourne404() throws Exception {
        mockMvc.perform(get("/api/employees/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Aucun salarié avec l'identifiant 999999"));
    }

    @Test
    void findById_salarieSupprimeLogiquement_retourne404() throws Exception {
        Employee deleted = save("E200", "Dupont", "Bruno");
        deleted.markDeleted();
        employeeRepository.save(deleted);

        mockMvc.perform(get("/api/employees/" + deleted.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_identifiantNonNumerique_retourne400() throws Exception {
        mockMvc.perform(get("/api/employees/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le paramètre « id » est invalide"));
    }

    /** Vérifie juste que les deux appels d'API ci-dessus renvoient bien des noms distincts, pas de collision fortuite. */
    @Test
    void search_deuxSalariesDistincts_sontTousDeuxPresents() throws Exception {
        save("E100", "Martin", "Alice");
        save("E200", "Dupont", "Bruno");

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nom", containsInAnyOrder("Martin", "Dupont")));
    }
}
