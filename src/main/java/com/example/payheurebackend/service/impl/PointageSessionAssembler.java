package com.example.payheurebackend.service.impl;

import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.dto.PointageSessionResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Apparie des badgeages bruts en sessions entrée/sortie. Partagé entre {@code PaieServiceImpl}
 * (un salarié à la fois) et {@code PointageAnomalieServiceImpl} (tous les salariés, à la
 * recherche des pointages incomplets) : les deux doivent détecter les anomalies exactement de la
 * même façon, pour qu'un badgeage sans sortie ne soit jamais compté comme travaillé sur l'un des
 * deux écrans et signalé comme anomalie sur l'autre.
 */
@Component
public class PointageSessionAssembler {

    /**
     * Apparie les badgeages jour par jour, dans l'ordre chronologique : le 1er badgeage d'une
     * journée est son entrée, le 2e sa sortie, le 3e une nouvelle entrée, etc. {@code pointages}
     * doit couvrir la/les journée(s) complète(s) (et non déjà tronqué à {@code fenetreDebut}/
     * {@code fenetreFin}) pour que l'appariement entrée/sortie soit correct même quand l'une des
     * deux bornes de la période demandée tombe au milieu d'une session.
     * <p>
     * Un badgeage sans sortie correspondante (nombre impair de badgeages ce jour-là, oubli de
     * pointer par ex.) est renvoyé comme anomalie et exclu du total travaillé. Seule la portion
     * d'une session qui chevauche {@code [fenetreDebut, fenetreFin]} est restituée ; les sessions
     * qui ne chevauchent pas du tout la fenêtre demandée sont omises. Une session dont l'entrée
     * et la sortie tombent à la même date/heure est conservée si cet instant est dans la fenêtre,
     * avec une durée de 0 minute.
     */
    public List<PointageSessionResponse> construire(
            List<Pointage> pointages, LocalDateTime fenetreDebut, LocalDateTime fenetreFin) {
        Map<LocalDate, List<Pointage>> byDay = pointages.stream()
                .collect(Collectors.groupingBy(p -> p.getDateHeure().toLocalDate(), LinkedHashMap::new, Collectors.toList()));

        List<PointageSessionResponse> sessions = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Pointage>> dayEntry : byDay.entrySet()) {
            List<Pointage> dayPointages = dayEntry.getValue();
            for (int i = 0; i < dayPointages.size(); i += 2) {
                LocalDateTime entree = dayPointages.get(i).getDateHeure();
                boolean hasSortie = i + 1 < dayPointages.size();

                if (!hasSortie) {
                    if (entree.isBefore(fenetreDebut) || entree.isAfter(fenetreFin)) {
                        continue; // badgeage orphelin hors de la période demandée : non pertinent ici
                    }
                    sessions.add(new PointageSessionResponse(dayEntry.getKey(), entree, null, 0, true));
                    continue;
                }

                LocalDateTime sortie = dayPointages.get(i + 1).getDateHeure();
                LocalDateTime debutChevauchement = entree.isAfter(fenetreDebut) ? entree : fenetreDebut;
                LocalDateTime finChevauchement = sortie.isBefore(fenetreFin) ? sortie : fenetreFin;
                // Une session instantanée (deux badgeages à la même date/heure, ex. double bip à
                // la borne ou saisie deux fois du même pointage) ne chevauche aucun intervalle au
                // sens strict : c'est son instant qu'il faut situer dans la fenêtre, pas son
                // chevauchement avec elle. Sans cette distinction elle serait écartée comme une
                // session hors période, et le salarié disparaîtrait de tous les écrans (calcul de
                // paie, anomalies, analytics, recherche par période) alors que ses badgeages sont
                // bien en base. Elle est donc restituée avec une durée de 0 minute.
                boolean dansLaFenetre = entree.equals(sortie)
                        ? !entree.isBefore(fenetreDebut) && !entree.isAfter(fenetreFin)
                        : debutChevauchement.isBefore(finChevauchement);
                if (!dansLaFenetre) {
                    continue; // session hors de la fenêtre demandée (aucun chevauchement)
                }

                long dureeMinutes = Duration.between(debutChevauchement, finChevauchement).toMinutes();
                sessions.add(new PointageSessionResponse(
                        dayEntry.getKey(), debutChevauchement, finChevauchement, dureeMinutes, false));
            }
        }
        return sessions;
    }
}
