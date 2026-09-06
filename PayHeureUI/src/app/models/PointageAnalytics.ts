/** Regroupement temporel des statistiques d'analytics (voir `pointage-analytics.component.ts`). */
export type Granularite = 'JOUR' | 'SEMAINE' | 'MOIS';

/**
 * `dateDebut`/`dateFin` au format `yyyy-MM-ddTHH:mm` (date et heure saisies séparément à
 * l'écran, concaténées avant l'envoi), comme {@link PointageAnomalieRequest}. Pas de salarié : la
 * recherche porte sur l'ensemble des salariés actifs.
 */
export interface PointageAnalyticsRequest {
  dateDebut: string;
  dateFin: string;
  granularite: Granularite;
}

/**
 * Agrégat des sessions de travail d'une sous-période (jour, semaine ou mois selon la granularité
 * demandée), tous salariés confondus.
 */
export interface PointageAnalyticsPeriode {
  /** Premier jour de la sous-période : le jour lui-même, le lundi de sa semaine, ou le 1er de son mois. */
  dateDebut: string;
  sessionsCompletes: number;
  sessionsIncompletes: number;
  /** Pourcentage (0-100) de sessions complètes parmi l'ensemble des sessions de la sous-période. */
  ratioCompletion: number;
}

/** Résultat des statistiques de complétude des pointages sur une période. */
export interface PointageAnalyticsResponse {
  granularite: Granularite;
  /** Une par sous-période couverte par la période demandée, dans l'ordre chronologique. */
  periodes: PointageAnalyticsPeriode[];
  totalSessionsCompletes: number;
  totalSessionsIncompletes: number;
  ratioCompletionGlobal: number;
}
