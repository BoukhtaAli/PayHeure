package com.example.payheurebackend.repository;

import com.example.payheurebackend.domain.Pointage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointageRepository extends JpaRepository<Pointage, Long> {

    /** Les badgeages d'un salarié sur une période, dans l'ordre chronologique. */
    List<Pointage> findByEmployeeIdAndDateHeureBetweenOrderByDateHeureAsc(
            Long employeeId, LocalDateTime start, LocalDateTime end);

    /**
     * Les badgeages de tous les salariés actifs (non supprimés logiquement) sur une période,
     * triés par salarié puis par date : regroupables directement salarié par salarié, dans
     * l'ordre chronologique de chacun, pour y rechercher les anomalies (voir
     * {@code PointageAnomalieServiceImpl}). Les salariés supprimés sont exclus comme partout
     * ailleurs (voir {@code EmployeeServiceImpl}), même si leurs pointages restent en base.
     */
    @Query("select p from Pointage p where p.employee.deletedAt is null and p.dateHeure between :debut and :fin "
            + "order by p.employee.id asc, p.dateHeure asc")
    List<Pointage> findActifsEntre(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /**
     * Comme {@link #findActifsEntre}, mais sans exclure les salariés supprimés logiquement : à
     * l'appelant de décider s'il les garde. Utilisé par {@code EmployeeServiceImpl}, où ce filtre
     * est déjà appliqué séparément via {@code EmployeeSpecifications} (le critère "supprimés
     * uniquement" doit pouvoir se combiner avec une période).
     */
    @Query("select p from Pointage p where p.dateHeure between :debut and :fin "
            + "order by p.employee.id asc, p.dateHeure asc")
    List<Pointage> findEntre(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    boolean existsByEmployeeId(Long employeeId);
}
