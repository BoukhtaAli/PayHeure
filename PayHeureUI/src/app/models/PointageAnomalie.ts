import { Employee } from './Employee';
import { Pointage } from './Pointage';

/**
 * `dateDebut`/`dateFin` au format `yyyy-MM-ddTHH:mm` (date et heure saisies séparément à
 * l'écran, concaténées avant l'envoi), comme {@link PaieCalculRequest}. Pas de salarié ni de
 * taux horaire : la recherche porte sur l'ensemble des salariés actifs.
 */
export interface PointageAnomalieRequest {
  dateDebut: string;
  dateFin: string;
}

/**
 * Une journée en anomalie (badgeage sans sortie correspondante) pour un salarié.
 */
export interface PointageAnomalieJour {
  date: string;
  /** Tous les badgeages bruts de cette journée, pas seulement le badgeage orphelin. */
  pointages: Pointage[];
}

/**
 * Un salarié ayant au moins un pointage incomplet (badgeage sans sortie correspondante) sur la
 * période demandée.
 */
export interface PointageAnomalieResponse {
  employee: Employee;
  /** Les seules journées en anomalie de ce salarié sur la période, jamais vide. */
  anomalies: PointageAnomalieJour[];
}
