/** Un badgeage brut, tel qu'enregistré en base (sans notion d'entrée/sortie). */
export interface Pointage {
  id: number;
  dateHeure: string;
}

/**
 * Une session de travail reconstituée à partir de deux badgeages consécutifs d'une même
 * journée (entrée, puis sortie).
 */
export interface PointageSession {
  date: string;
  heureEntree: string;
  heureSortie: string | null;
  dureeMinutes: number;
  /** `true` si `heureSortie` est absente (badgeage sans sortie correspondante). */
  anomalie: boolean;
}
