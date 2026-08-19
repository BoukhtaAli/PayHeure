package com.example.payheurebackend.config;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.repository.EmployeeRepository;
import com.example.payheurebackend.repository.PointageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sème quelques salariés et badgeages de démonstration au premier démarrage, uniquement si la
 * table {@code employee} est vide : permet de tester l'écran de calcul de paie sans devoir
 * saisir des pointages à la main. Désactivable via {@code payheure.demo.seed-enabled=false}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final PointageRepository pointageRepository;
    private final DemoDataProperties demoDataProperties;

    @Override
    @Transactional
    public void run(String... args) {
        if (!demoDataProperties.seedEnabled() || employeeRepository.count() > 0) {
            return;
        }

        log.info("Base de pointage vide : génération d'un jeu de données de démonstration");

        Employee amel = save("E001", "Boukhta", "Amel");
        Employee youssef = save("E002", "El Amrani", "Youssef");
        Employee sara = save("E003", "Idrissi", "Sara");

        // 5 derniers jours ouvrés, journée standard 08h-12h / 13h30-17h30 (7h30 travaillées/jour).
        List<LocalDate> lastFiveDays = lastBusinessDays(5);
        for (LocalDate day : lastFiveDays) {
            punchDay(amel, day, LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(13, 30), LocalTime.of(17, 30));
            punchDay(youssef, day, LocalTime.of(8, 15), LocalTime.of(12, 0), LocalTime.of(13, 0), LocalTime.of(17, 0));
        }

        // Sara illustre une anomalie : sortie oubliée le dernier jour (nombre impair de badgeages).
        for (int i = 0; i < lastFiveDays.size() - 1; i++) {
            punchDay(sara, lastFiveDays.get(i), LocalTime.of(9, 0), LocalTime.of(13, 0), LocalTime.of(14, 0), LocalTime.of(18, 0));
        }
        pointage(sara, lastFiveDays.get(lastFiveDays.size() - 1).atTime(9, 0));
    }

    private Employee save(String matricule, String nom, String prenom) {
        return employeeRepository.save(Employee.builder().matricule(matricule).nom(nom).prenom(prenom).build());
    }

    private void punchDay(Employee employee, LocalDate day, LocalTime... times) {
        for (LocalTime time : times) {
            pointage(employee, day.atTime(time));
        }
    }

    private void pointage(Employee employee, LocalDateTime dateHeure) {
        pointageRepository.save(Pointage.builder().employee(employee).dateHeure(dateHeure).build());
    }

    /** Les {@code count} derniers jours ouvrés (hors samedi/dimanche), du plus ancien au plus récent. */
    private List<LocalDate> lastBusinessDays(int count) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate cursor = LocalDate.now();
        while (days.size() < count) {
            if (cursor.getDayOfWeek().getValue() < 6) {
                days.add(cursor);
            }
            cursor = cursor.minusDays(1);
        }
        Collections.reverse(days);
        return days;
    }
}
