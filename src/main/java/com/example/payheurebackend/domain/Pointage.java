package com.example.payheurebackend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Un badgeage brut du salarié : une date/heure, sans type entrée/sortie. La bdd de pointage
 * ne distingue pas les deux ; c'est {@code PaieServiceImpl} qui les détermine en appariant les
 * badgeages d'une même journée dans l'ordre chronologique (1er = entrée, 2e = sortie, etc.).
 */
@Entity
@Table(name = "pointage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pointage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;
}
