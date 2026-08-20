package com.example.payheurebackend.api;

import com.example.payheurebackend.dto.PointageCreateRequest;
import com.example.payheurebackend.dto.PointageResponse;
import com.example.payheurebackend.service.PointageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Enregistrement des pointages (badgeages bruts) d'un salarié. */
@RestController
@RequestMapping("/api/pointages")
@RequiredArgsConstructor
public class PointageController {

    private final PointageService pointageService;

    /** Ajoute un badgeage pour le salarié désigné dans la requête. */
    @PostMapping
    public ResponseEntity<PointageResponse> creer(@Valid @RequestBody PointageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pointageService.creer(request));
    }
}
