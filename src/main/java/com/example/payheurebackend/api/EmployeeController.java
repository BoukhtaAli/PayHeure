package com.example.payheurebackend.api;

import com.example.payheurebackend.dto.EmployeeResponse;
import com.example.payheurebackend.dto.EmployeeSearchCriteria;
import com.example.payheurebackend.dto.PageResponse;
import com.example.payheurebackend.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** Recherche des salariés, préalable au calcul de paie. */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /** Taille de page appliquée quand le client n'en précise pas. */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /** Taille de page maximale, pour éviter qu'un client ne demande tous les salariés d'un coup. */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Les salariés dont le matricule, le nom ou le prénom contient le texte recherché, une page
     * à la fois. Sans paramètre, renvoie la première page de tous les salariés actifs.
     * {@code dateDebut}/{@code dateFin} restreignent en plus aux salariés ayant au moins un
     * pointage dans cette période (les deux bornes doivent être fournies pour filtrer).
     */
    @GetMapping
    public PageResponse<EmployeeResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        return employeeService.search(new EmployeeSearchCriteria(query, dateDebut, dateFin, null), pageable);
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    @GetMapping("/{id}")
    public EmployeeResponse findById(@PathVariable Long id) {
        return employeeService.findById(id);
    }
}
