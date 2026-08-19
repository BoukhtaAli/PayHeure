package com.example.payheurebackend.repository;

import com.example.payheurebackend.domain.Pointage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PointageRepository extends JpaRepository<Pointage, Long> {

    /** Les badgeages d'un salarié sur une période, dans l'ordre chronologique. */
    List<Pointage> findByEmployeeIdAndDateHeureBetweenOrderByDateHeureAsc(
            Long employeeId, LocalDateTime start, LocalDateTime end);

    boolean existsByEmployeeId(Long employeeId);
}
