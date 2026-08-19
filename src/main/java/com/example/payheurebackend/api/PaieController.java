package com.example.payheurebackend.api;

import com.example.payheurebackend.dto.PaieCalculRequest;
import com.example.payheurebackend.dto.PaieCalculResponse;
import com.example.payheurebackend.service.PaieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Calcul de la paie d'un salarié à partir de ses pointages. */
@RestController
@RequestMapping("/api/paie")
@RequiredArgsConstructor
public class PaieController {

    private final PaieService paieService;

    /**
     * Récupère les pointages du salarié sur la période demandée et calcule le montant dû, au
     * taux horaire fourni dans la requête (jamais stocké côté serveur).
     */
    @PostMapping("/calcul")
    public PaieCalculResponse calculer(@Valid @RequestBody PaieCalculRequest request) {
        return paieService.calculer(request);
    }
}
