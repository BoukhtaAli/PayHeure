import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;

/**
 * Script jetable (pas un test, pas commité dans le suivi normal du build) : peuple
 * C:/PayHeure/payheureDb.accdb avec 3 salariés fictifs et leurs badgeages du 1er septembre 2026
 * à aujourd'hui, tous les jours (weekends inclus), pour tester l'écran d'analytics. Idempotent :
 * relancer le script supprime d'abord les données précédemment injectées par ce même script
 * (matricules TESTAN1..3) avant de les recréer.
 */
public class SeedAnalyticsData {

    private static final String URL = "jdbc:ucanaccess://C:/PayHeure/payheureDb.accdb;newDatabaseVersion=V2010";
    private static final String[] MATRICULES = {"TESTAN1", "TESTAN2", "TESTAN3"};
    private static final String[] NOMS = {"Analytics", "Analytics", "Analytics"};
    private static final String[] PRENOMS = {"Test1", "Test2", "Test3"};

    public static void main(String[] args) throws Exception {
        try (Connection cn = DriverManager.getConnection(URL)) {
            cn.setAutoCommit(false);

            // Nettoyage des données d'une exécution précédente du script.
            List<Long> anciens = idsExistants(cn);
            if (!anciens.isEmpty()) {
                try (PreparedStatement del = cn.prepareStatement("DELETE FROM pointage WHERE employee_id = ?")) {
                    for (Long id : anciens) { del.setLong(1, id); del.executeUpdate(); }
                }
                try (PreparedStatement del = cn.prepareStatement("DELETE FROM employee WHERE id = ?")) {
                    for (Long id : anciens) { del.setLong(1, id); del.executeUpdate(); }
                }
                System.out.println("Nettoyé " + anciens.size() + " salarié(s) test existant(s).");
            }

            // Création des 3 salariés fictifs.
            long[] ids = new long[MATRICULES.length];
            try (PreparedStatement ins = cn.prepareStatement(
                    "INSERT INTO employee (matricule, nom, prenom) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 0; i < MATRICULES.length; i++) {
                    ins.setString(1, MATRICULES[i]);
                    ins.setString(2, NOMS[i]);
                    ins.setString(3, PRENOMS[i]);
                    ins.executeUpdate();
                    try (ResultSet keys = ins.getGeneratedKeys()) {
                        keys.next();
                        ids[i] = keys.getLong(1);
                    }
                }
            }
            System.out.println("Salariés créés : " + Arrays.toString(ids));

            // Tous les jours du 1er septembre 2026 à aujourd'hui, weekends inclus (borne haute =
            // aujourd'hui, réelle date système, pas figée sur 2026-09-07, au cas où le script
            // serait relancé plus tard).
            LocalDate debut = LocalDate.of(2026, 9, 1);
            LocalDate fin = LocalDate.now();
            List<LocalDate> jours = new ArrayList<>();
            for (LocalDate d = debut; !d.isAfter(fin); d = d.plusDays(1)) {
                jours.add(d);
            }
            System.out.println("Jours couverts : " + jours);

            try (PreparedStatement ins = cn.prepareStatement(
                    "INSERT INTO pointage (employee_id, date_heure) VALUES (?, ?)")) {
                Random random = new Random(42); // graine fixe : jeu de données reproductible
                int compteur = 0;
                for (LocalDate jour : jours) {
                    for (long employeeId : ids) {
                        // Entrée le matin, toujours présente.
                        compteur += badge(ins, employeeId, jour, 8, random.nextInt(20));
                        // Sortie le soir, absente ~1 fois sur 5 (session incomplète, pour peupler
                        // le graphique "Complets / Incomplets" et l'écran d'anomalies).
                        if (random.nextInt(5) != 0) {
                            compteur += badge(ins, employeeId, jour, 17, random.nextInt(30));
                        }
                    }
                }
                System.out.println("Badgeages insérés : " + compteur);
            }

            cn.commit();
            System.out.println("OK.");
        }
    }

    private static int badge(PreparedStatement ins, long employeeId, LocalDate jour, int heure, int minute) throws SQLException {
        ins.setLong(1, employeeId);
        ins.setTimestamp(2, Timestamp.valueOf(LocalDateTime.of(jour, java.time.LocalTime.of(heure, minute))));
        return ins.executeUpdate();
    }

    private static List<Long> idsExistants(Connection cn) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement sel = cn.prepareStatement(
                "SELECT id FROM employee WHERE matricule IN (?, ?, ?)")) {
            for (int i = 0; i < MATRICULES.length; i++) sel.setString(i + 1, MATRICULES[i]);
            try (ResultSet rs = sel.executeQuery()) {
                while (rs.next()) ids.add(rs.getLong(1));
            }
        }
        return ids;
    }
}
