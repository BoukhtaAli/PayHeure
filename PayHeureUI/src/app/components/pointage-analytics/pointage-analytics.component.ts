import { Component, OnDestroy, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ChartConfiguration, ChartData } from 'chart.js';
import { Subscription } from 'rxjs';
import { Granularite, PointageAnalyticsPeriode, PointageAnalyticsResponse } from '../../models/PointageAnalytics';
import { PointageAnalyticsService } from '../../services/pointage-analytics.service';
import { LanguageService } from '../../services/language.service';
import { horodatageLocal, telechargerCsv } from '../../utils/csv';
import { BreadcrumbItem } from '../breadcrumb/breadcrumb.component';

/**
 * Vue choisie à l'écran : détermine l'étendue de la période envoyée au backend (un jour, une
 * semaine ou un mois), pas la granularité d'agrégation, qui reste toujours journalière (voir
 * `charger`) pour pouvoir tracer un graphique jour par jour quelle que soit la vue. La période
 * elle-même est ancrée sur `ancre` (voir le composant), pas figée sur "aujourd'hui" : `naviguer`
 * permet de la faire glisser dans le passé (ex. le mois précédent) ou de revenir en avant.
 */
export type PeriodePreset = 'JOUR' | 'SEMAINE' | 'MOIS';

/** Couleurs de statut (bon/avertissement) de la charte dataviz : jamais recyclées pour une autre série. */
const COULEUR_COMPLET = '#0ca30c';
const COULEUR_INCOMPLET = '#fab219';

/** Minuit, heure locale, du jour donné. */
function debutJour(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0);
}

/** 23:59:59, heure locale, du jour donné. */
function finJour(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59);
}

/** Lundi (minuit) de la semaine ISO contenant la date donnée. */
function lundiDeLaSemaine(date: Date): Date {
  const jour = date.getDay(); // 0 = dimanche ... 6 = samedi
  const decalage = jour === 0 ? -6 : 1 - jour;
  const lundi = new Date(date);
  lundi.setDate(date.getDate() + decalage);
  return debutJour(lundi);
}

/** Dimanche (23:59:59) de la semaine ISO contenant la date donnée. */
function dimancheDeLaSemaine(date: Date): Date {
  const lundi = lundiDeLaSemaine(date);
  const dimanche = new Date(lundi);
  dimanche.setDate(lundi.getDate() + 6);
  return finJour(dimanche);
}

/** `yyyy-MM-ddTHH:mm:ss` en heure locale, format `LocalDateTime` attendu par le backend. */
function toIsoLocalDateTime(date: Date): string {
  const deuxChiffres = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${deuxChiffres(date.getMonth() + 1)}-${deuxChiffres(date.getDate())}`
    + `T${deuxChiffres(date.getHours())}:${deuxChiffres(date.getMinutes())}:${deuxChiffres(date.getSeconds())}`;
}

/** Bornes de la période à envoyer au backend pour la vue choisie, ancrée sur `ancre`. */
function bornes(preset: PeriodePreset, ancre: Date): { debut: Date; fin: Date } {
  switch (preset) {
    case 'SEMAINE':
      return { debut: lundiDeLaSemaine(ancre), fin: dimancheDeLaSemaine(ancre) };
    case 'MOIS':
      return {
        debut: debutJour(new Date(ancre.getFullYear(), ancre.getMonth(), 1)),
        fin: finJour(new Date(ancre.getFullYear(), ancre.getMonth() + 1, 0))
      };
    default:
      return { debut: debutJour(ancre), fin: finJour(ancre) };
  }
}

/** Décale l'ancre d'une unité (jour, semaine ou mois selon la vue), en avant ou en arrière. */
function decaler(preset: PeriodePreset, ancre: Date, sens: 1 | -1): Date {
  const resultat = new Date(ancre);
  switch (preset) {
    case 'SEMAINE':
      resultat.setDate(resultat.getDate() + sens * 7);
      break;
    case 'MOIS':
      resultat.setMonth(resultat.getMonth() + sens);
      break;
    default:
      resultat.setDate(resultat.getDate() + sens);
  }
  return resultat;
}

/**
 * Écran d'analytics des pointages : pas de saisie de dates, seulement une vue (jour/semaine/mois)
 * et une navigation (précédent/suivant/aujourd'hui, voir `naviguer`) pour la faire glisser dans le
 * temps — ex. consulter le mois précédent — et une représentation visuelle (compteurs, jauge de
 * ratio, graphique en barres empilées jour par jour) plutôt qu'un tableau de salariés comme
 * l'écran d'anomalies. La granularité envoyée au backend reste toujours journalière : c'est la vue
 * qui fait varier l'étendue de la période, pas la taille des sous-périodes agrégées.
 */
@Component({
  selector: 'app-pointage-analytics',
  templateUrl: './pointage-analytics.component.html',
  styleUrls: ['./pointage-analytics.component.css']
})
export class PointageAnalyticsComponent implements OnInit, OnDestroy {

  readonly presets: PeriodePreset[] = ['JOUR', 'SEMAINE', 'MOIS'];

  readonly breadcrumbItems: BreadcrumbItem[] = [
    { labelKey: 'NAV.HOME', link: ['/home'] },
    { labelKey: 'NAV.ANALYTICS' }
  ];

  preset: PeriodePreset = 'JOUR';

  /** Date sur laquelle la période affichée est ancrée ; `naviguer`/`allerAujourdhui` la déplacent. */
  private ancre: Date = new Date();

  result: PointageAnalyticsResponse | null = null;
  loading = false;
  /**
   * Message d'erreur brut (déjà traduit côté serveur, voir `errorMessageKey`), affiché tel quel.
   * `errorMessageKey` a priorité côté template : les deux ne sont jamais renseignés en même temps.
   */
  errorMessage: string | null = null;

  /**
   * Clé i18n de l'erreur à afficher, traduite dans le template via le pipe `translate` — donc
   * réactive à un changement de langue, contrairement à un texte déjà résolu par
   * `translate.instant()` et stocké tel quel (piège dans lequel `errorMessage` ne doit pas tomber).
   */
  errorMessageKey: string | null = null;

  /** Bascule entre le graphique et son équivalent accessible (tableau), voir `anti-patterns.md`. */
  afficherTableau = false;

  readonly barChartType = 'bar' as const;
  barChartData: ChartData<'bar'> = { labels: [], datasets: [] };

  // Deux jeux d'options selon la vue (voir `construireGraphique`) : la vue "jour" trace une
  // comparaison à deux barres (empilement inutile, une seule série donc pas de légende), les vues
  // "semaine"/"mois" tracent un empilement complet/incomplet jour par jour (légende obligatoire,
  // deux séries).
  barChartOptions: ChartConfiguration<'bar'>['options'] = PointageAnalyticsComponent.optionsComparaison();

  private static optionsComparaison(): ChartConfiguration<'bar'>['options'] {
    return {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: { grid: { display: false } },
        // precision: 0 : des sessions entières, jamais de graduation à virgule.
        y: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: '#e1e0d9' } }
      },
      plugins: {
        // Une seule série, chaque barre déjà identifiée par son étiquette d'axe X (Complets /
        // Incomplets) : pas de boîte de légende à côté (voir marks-and-anatomy.md du skill
        // dataviz, "un seul jeu de données n'a pas besoin de légende").
        legend: { display: false }
      }
    };
  }

  private static optionsEmpilement(): ChartConfiguration<'bar'>['options'] {
    return {
      responsive: true,
      maintainAspectRatio: false,
      // 'index' + intersect: false : le survol d'une colonne affiche les deux séries de ce jour-là
      // dans une seule infobulle, pas seulement celle sous le curseur.
      interaction: { mode: 'index', intersect: false },
      scales: {
        x: { stacked: true, grid: { display: false } },
        y: { stacked: true, beginAtZero: true, ticks: { precision: 0 }, grid: { color: '#e1e0d9' } }
      },
      plugins: {
        legend: {
          position: 'top',
          labels: { usePointStyle: true, boxWidth: 8, padding: 16 },
          // Les items de légende sont cliquables (ils filtrent la série complète/incomplète
          // affichée) : sans ceci Chart.js laisse le curseur par défaut, rien n'indique qu'ils
          // sont interactifs.
          onHover: (event) => {
            if (event.native?.target instanceof HTMLElement) event.native.target.style.cursor = 'pointer';
          },
          onLeave: (event) => {
            if (event.native?.target instanceof HTMLElement) event.native.target.style.cursor = 'default';
          }
        },
        tooltip: { mode: 'index', intersect: false }
      }
    };
  }

  /**
   * Les libellés du graphique (jours de semaine, "Complets"/"Incomplets") sont calculés une fois,
   * à la construction du jeu de données (voir `construireGraphique`) — contrairement au reste de
   * l'écran, ils ne passent pas par le pipe `translate` du template, donc ne se retraduisent pas
   * tout seuls. Ce flux réagit aux changements de langue pour reconstruire le graphique déjà
   * affiché sans attendre un rechargement (changement de vue ou de période).
   */
  private langChangeSub?: Subscription;

  constructor(
    private readonly pointageAnalyticsService: PointageAnalyticsService,
    private readonly translate: TranslateService,
    private readonly languageService: LanguageService
  ) {}

  ngOnInit(): void {
    this.charger();
    this.langChangeSub = this.translate.onLangChange.subscribe(() => {
      if (this.result) this.construireGraphique(this.result);
    });
  }

  ngOnDestroy(): void {
    this.langChangeSub?.unsubscribe();
  }

  /**
   * Sens d'écriture de la langue courante. Les flèches précédent/suivant marquent un sens de
   * lecture temporelle, pas une position absolue gauche/droite : en arabe (RTL), le passé est à
   * droite et l'avenir à gauche, donc "précédent" doit pointer vers la droite (voir le template).
   */
  get rtl(): boolean {
    return this.languageService.currentLang.dir === 'rtl';
  }

  /**
   * Change de vue (jour/semaine/mois) sans changer l'ancre : rester sur le mois affiché en
   * passant en vue "jour" par exemple retombe sur son 1er jour, pas sur aujourd'hui.
   */
  selectionner(preset: PeriodePreset): void {
    this.preset = preset;
    this.afficherTableau = false;
    this.charger();
  }

  /** Fait glisser la période affichée d'une unité (jour, semaine ou mois selon la vue). */
  naviguer(sens: 1 | -1): void {
    this.ancre = decaler(this.preset, this.ancre, sens);
    this.afficherTableau = false;
    this.charger();
  }

  /** Revient sur la période courante (celle contenant aujourd'hui), quelle que soit la vue. */
  allerAujourdhui(): void {
    this.ancre = new Date();
    this.afficherTableau = false;
    this.charger();
  }

  /**
   * Désactive "suivant" une fois revenu sur la période courante : naviguer plus loin afficherait
   * une période future, forcément vide (aucun pointage n'existe encore pour demain).
   */
  get suivantDesactive(): boolean {
    return bornes(this.preset, this.ancre).fin >= finJour(new Date());
  }

  /** Libellé de la période affichée, au-dessus du graphique/tableau : ex. "06-09-2026", "31-08-2026 – 06-09-2026" ou "septembre 2026". */
  get libellePeriode(): string {
    const { debut, fin } = bornes(this.preset, this.ancre);
    // Toujours en 'fr-FR', même écran en arabe : la locale 'ar' d'Intl produit des chiffres
    // indo-arabes et un ordre jour/mois/année qui, combinés au sens d'écriture RTL de la page,
    // affichent une date brouillée (voir capture utilisateur) malgré le `dir="ltr"` du template.
    // Un format fixe, comme les saisies `dd-MM-yyyy` du reste de l'appli (voir date-format.ts),
    // évite complètement le problème.
    const jourMoisAnnee = (date: Date) => date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });

    if (this.preset === 'MOIS') return debut.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
    if (this.preset === 'SEMAINE') return `${jourMoisAnnee(debut)} – ${jourMoisAnnee(fin)}`;
    return jourMoisAnnee(debut);
  }

  get periodes(): PointageAnalyticsPeriode[] {
    return this.result?.periodes ?? [];
  }

  private charger(): void {
    this.loading = true;
    this.errorMessage = null;
    this.errorMessageKey = null;
    const { debut, fin } = bornes(this.preset, this.ancre);
    const granularite: Granularite = 'JOUR';

    this.pointageAnalyticsService.calculer({
      dateDebut: toIsoLocalDateTime(debut),
      dateFin: toIsoLocalDateTime(fin),
      granularite
    }).subscribe({
      next: result => {
        this.result = result;
        this.construireGraphique(result);
        this.loading = false;
      },
      error: error => {
        // Le résultat précédent n'est pas effacé au clic sur un préréglage tant que la réponse
        // n'est pas arrivée (voir `.chargement` dans le template) ; en cas d'erreur en revanche,
        // il n'y a plus rien de fiable à montrer.
        this.result = null;
        this.loading = false;
        // Le backend ne répond qu'en français (voir GlobalExceptionHandler) et ce message n'est
        // pas traduit ; `errorMessage` (texte déjà résolu) sert seulement à ce cas brut venu du
        // backend, `errorMessageKey` (clé i18n) au message générique, traduit dans le template via
        // le pipe `translate` pour rester à jour si l'utilisateur change de langue ensuite.
        const messageBrut = error?.error?.message;
        this.errorMessage = messageBrut ?? null;
        this.errorMessageKey = messageBrut ? null : 'ANALYTICS.SEARCH_ERROR';
      }
    });
  }

  private construireGraphique(result: PointageAnalyticsResponse): void {
    if (this.preset === 'JOUR') {
      // Une seule journée : pas de comparaison possible dans le temps (un empilement à une seule
      // barre serait un anti-pattern, voir `anti-patterns.md`), mais comparer les deux compteurs
      // entre eux reste une lecture de magnitude tout à fait valable — deux barres, pas une.
      this.barChartOptions = PointageAnalyticsComponent.optionsComparaison();
      this.barChartData = {
        labels: [this.translate.instant('ANALYTICS.STAT_COMPLETE'), this.translate.instant('ANALYTICS.STAT_INCOMPLETE')],
        datasets: [{
          data: [result.totalSessionsCompletes, result.totalSessionsIncompletes],
          backgroundColor: [COULEUR_COMPLET, COULEUR_INCOMPLET],
          borderColor: '#fff',
          borderWidth: 2,
          borderRadius: 4,
          maxBarThickness: 48
        }]
      };
      return;
    }

    this.barChartOptions = PointageAnalyticsComponent.optionsEmpilement();
    this.barChartData = {
      labels: result.periodes.map(periode => this.libelleJour(periode.dateDebut)),
      datasets: [
        {
          label: this.translate.instant('ANALYTICS.STAT_COMPLETE'),
          data: result.periodes.map(periode => periode.sessionsCompletes),
          backgroundColor: COULEUR_COMPLET,
          // Liseré blanc (couleur de fond du panneau) plutôt qu'un contour : matérialise le
          // séparateur de 2px entre segments empilés sans ajouter d'encre de donnée (voir
          // marks-and-anatomy.md du skill dataviz).
          borderColor: '#fff',
          borderWidth: 2,
          borderRadius: 4,
          maxBarThickness: 24,
          stack: 'sessions'
        },
        {
          label: this.translate.instant('ANALYTICS.STAT_INCOMPLETE'),
          data: result.periodes.map(periode => periode.sessionsIncompletes),
          backgroundColor: COULEUR_INCOMPLET,
          borderColor: '#fff',
          borderWidth: 2,
          borderRadius: 4,
          maxBarThickness: 24,
          stack: 'sessions'
        }
      ]
    };
  }

  /**
   * Libellé d'une sous-période sur l'axe du graphique et dans le tableau : jour de semaine abrégé
   * suivi du quantième (ex. "lun. 06"), pour les deux vues "semaine" et "mois" — le mois lui-même
   * n'a pas besoin d'être répété ici, il est déjà donné par `libellePeriode` au-dessus.
   */
  libelleJour(dateIso: string): string {
    const date = new Date(dateIso);
    const locale = this.translate.currentLang || 'fr-FR';
    const jourSemaine = date.toLocaleDateString(locale, { weekday: 'short' });
    const quantieme = date.toLocaleDateString(locale, { day: '2-digit' });
    return `${jourSemaine} ${quantieme}`;
  }

  /** Exporte les sous-périodes du résultat affiché (déjà en mémoire, pas d'appel serveur pour ça). */
  telechargerCsv(): void {
    if (!this.result) return;

    const entetes = ['COL_PERIOD', 'COL_COMPLETE', 'COL_INCOMPLETE', 'COL_RATIO']
      .map(cle => this.translate.instant(`ANALYTICS.${cle}`));

    const lignes = this.periodes.map(periode => [
      this.libelleJour(periode.dateDebut),
      String(periode.sessionsCompletes),
      String(periode.sessionsIncompletes),
      `${periode.ratioCompletion}%`
    ]);

    telechargerCsv(`analytics-pointage-${horodatageLocal(new Date())}.csv`, entetes, lignes);
  }
}
