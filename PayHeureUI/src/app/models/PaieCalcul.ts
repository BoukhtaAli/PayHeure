import { Employee } from './Employee';
import { Pointage, PointageSession } from './Pointage';

/**
 * Le taux horaire n'est jamais persisté côté serveur : il est saisi à chaque calcul.
 * `dateDebut`/`dateFin` sont au format `yyyy-MM-ddTHH:mm` (date et heure saisies séparément à
 * l'écran, concaténées avant l'envoi) : la période est bornée à l'heure et à la minute près,
 * pas seulement au jour.
 */
export interface PaieCalculRequest {
  employeeId: number;
  dateDebut: string;
  dateFin: string;
  tauxHoraire: number;
}

export interface PaieCalculResponse {
  employee: Employee;
  dateDebut: string;
  dateFin: string;
  tauxHoraire: number;
  pointages: Pointage[];
  sessions: PointageSession[];
  totalMinutes: number;
  totalDureeFormatee: string;
  montantTotal: number;
  dateHeureCalcul: string;
}
