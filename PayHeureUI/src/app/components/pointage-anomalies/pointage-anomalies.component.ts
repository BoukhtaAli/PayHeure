import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { Options } from 'flatpickr/dist/types/options';
import { PointageAnomalieJour, PointageAnomalieResponse } from '../../models/PointageAnomalie';
import { PointageAnomalieService } from '../../services/pointage-anomalie.service';
import { DATE_PATTERN, HEURE_PATTERN, toIsoDateTime } from '../../utils/date-format';
import { horodatageLocal, telechargerCsv } from '../../utils/csv';
import { BreadcrumbItem } from '../breadcrumb/breadcrumb.component';

/**
 * La comparaison se fait en chaîne (pas `new Date(...)`) : une fois convertie en
 * `yyyy-MM-ddTHH:mm`, zéro-paddée, l'ordre lexical suit exactement l'ordre chronologique, sans
 * les pièges de fuseau horaire d'un parsing `Date`. Même validateur que l'écran de calcul de
 * paie (voir paie-calcul.component.ts), dupliqué ici faute d'un module de formulaires partagé.
 */
function periodValidator(group: AbstractControl): ValidationErrors | null {
  const { dateDebut, heureDebut, dateFin, heureFin } = group.value;
  const champsValides = DATE_PATTERN.test(dateDebut) && HEURE_PATTERN.test(heureDebut)
    && DATE_PATTERN.test(dateFin) && HEURE_PATTERN.test(heureFin);
  if (!champsValides) return null;

  const debut = toIsoDateTime(dateDebut, heureDebut);
  const fin = toIsoDateTime(dateFin, heureFin);
  return fin < debut ? { invalidPeriod: true } : null;
}

/**
 * Écran de recherche des pointages incomplets ou oubliés : contrairement au calcul de paie, il
 * ne porte pas sur un ou plusieurs salariés choisis au préalable, mais balaie d'emblée l'ensemble
 * des salariés actifs sur la période demandée (voir PointageAnomalieService, un seul appel
 * serveur) pour n'afficher que ceux ayant au moins un badgeage sans sortie correspondante.
 */
@Component({
  selector: 'app-pointage-anomalies',
  templateUrl: './pointage-anomalies.component.html',
  styleUrls: ['./pointage-anomalies.component.css']
})
export class PointageAnomaliesComponent {

  /**
   * Caractère séparant les heures de badgeage d'une même journée dans la colonne "entrées" du
   * CSV exporté (voir `telechargerCsv`). Ni `;` (délimiteur de colonnes du CSV, voir
   * `utils/csv.ts`) ni `,` (séparateur décimal en locale française), pour rester sans ambiguïté
   * à l'ouverture dans Excel.
   */
  private static readonly SEPARATEUR_ENTREES = ' | ';

  /**
   * Taille de page du tableau de résultats. Pagination purement côté client : contrairement au
   * calcul de paie, cet écran n'a qu'un seul appel serveur qui renvoie déjà tous les salariés en
   * anomalie sur la période (voir `rechercher`), il n'y a donc pas de `PageResponse` à relayer.
   */
  private static readonly TAILLE_PAGE = 10;

  readonly breadcrumbItems: BreadcrumbItem[] = [
    { labelKey: 'NAV.HOME', link: ['/home'] },
    { labelKey: 'NAV.ANOMALIES' }
  ];

  /** `d-m-Y` est la syntaxe de formatage propre à flatpickr (équivalent de notre `dd-MM-yyyy`). */
  readonly dateOptions: Partial<Options> = {
    dateFormat: 'd-m-Y'
  };

  /** `noCalendar` + `time_24hr` : horloge seule, toujours en 24h, jamais de calendrier ni d'AM/PM. */
  readonly heureOptions: Partial<Options> = {
    enableTime: true,
    noCalendar: true,
    dateFormat: 'H:i',
    time_24hr: true
  };

  results: PointageAnomalieResponse[] = [];
  searched = false;
  errorMessage: string | null = null;

  /**
   * Une ligne par journée en anomalie, tous salariés confondus, triée par date décroissante (voir
   * `rechercher`). Calculée une seule fois à la recherche plutôt qu'un getter recalculé à chaque
   * cycle de détection de changements : `results` reste groupé par salarié (utile pour le CSV et
   * `totalAnomalies`), `lignes` sert uniquement à l'affichage du tableau.
   */
  lignes: { result: PointageAnomalieResponse; jour: PointageAnomalieJour }[] = [];

  /** Page courante du tableau de résultats, 0-indexée (alignée sur `app-pagination`). */
  page = 0;

  /**
   * Clé (`employeeId-date`) de la seule ligne actuellement dépliée, ou `null` si aucune : voir
   * `toggle`/`estDeplie`. Un seul champ plutôt qu'une `Set` — en ouvrir une referme les autres
   * (comportement accordéon), pas d'affichage simultané de plusieurs détails.
   */
  private deplie: string | null = null;

  readonly form: FormGroup = this.fb.group({
    dateDebut: ['', [Validators.required, Validators.pattern(DATE_PATTERN)]],
    heureDebut: ['', [Validators.required, Validators.pattern(HEURE_PATTERN)]],
    dateFin: ['', [Validators.required, Validators.pattern(DATE_PATTERN)]],
    heureFin: ['', [Validators.required, Validators.pattern(HEURE_PATTERN)]]
  }, { validators: [periodValidator] });

  constructor(
    private readonly fb: FormBuilder,
    private readonly pointageAnomalieService: PointageAnomalieService,
    private readonly translate: TranslateService
  ) {}

  /** Nombre total d'anomalies tous salariés confondus, affiché dans le titre des résultats. */
  get totalAnomalies(): number {
    return this.results.reduce((total, result) => total + result.anomalies.length, 0);
  }

  get totalPages(): number {
    return Math.ceil(this.lignes.length / PointageAnomaliesComponent.TAILLE_PAGE);
  }

  /** Lignes de la page courante, seules affichées dans le tableau de résultats. */
  get lignesPage(): { result: PointageAnomalieResponse; jour: PointageAnomalieJour }[] {
    const debut = this.page * PointageAnomaliesComponent.TAILLE_PAGE;
    return this.lignes.slice(debut, debut + PointageAnomaliesComponent.TAILLE_PAGE);
  }

  rechercher(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage = null;
    const { dateDebut, heureDebut, dateFin, heureFin } = this.form.value;

    this.pointageAnomalieService.lister({
      dateDebut: toIsoDateTime(dateDebut, heureDebut),
      dateFin: toIsoDateTime(dateFin, heureFin)
    }).subscribe({
      next: results => {
        this.results = results;
        // Le backend renvoie les journées en anomalie par ordre chronologique (voir
        // PointageAnomalieServiceImpl) et groupées par salarié ; on affiche ici une liste plate,
        // tous salariés confondus, de la plus récente à la plus ancienne. Comparaison en chaîne
        // (yyyy-MM-dd) : ordre lexical = ordre chronologique.
        this.lignes = results
          .flatMap(result => result.anomalies.map(jour => ({ result, jour })))
          .sort((a, b) => b.jour.date.localeCompare(a.jour.date));
        this.searched = true;
        this.deplie = null;
        this.page = 0;
      },
      error: error => {
        this.results = [];
        this.lignes = [];
        this.searched = true;
        this.page = 0;
        // Le backend ne répond qu'en français (voir GlobalExceptionHandler) et ce message n'est
        // pas traduit, contrairement au reste de l'écran. Le seul cas facilement prévisible
        // (période invalide) est intercepté avant l'appel par `periodValidator` ; ce message
        // générique ne sert plus que pour les erreurs réellement inattendues côté serveur.
        this.errorMessage = error?.error?.message ?? this.translate.instant('ANOMALIES.SEARCH_ERROR');
      }
    });
  }

  private cle(result: PointageAnomalieResponse, jour: PointageAnomalieJour): string {
    return `${result.employee.id}-${jour.date}`;
  }

  /**
   * Affiche le détail des badgeages bruts de cette journée, en repliant l'éventuelle autre ligne
   * déjà ouverte (une seule ligne dépliée à la fois, voir `deplie`).
   */
  toggle(result: PointageAnomalieResponse, jour: PointageAnomalieJour): void {
    const cle = this.cle(result, jour);
    this.deplie = this.deplie === cle ? null : cle;
  }

  estDeplie(result: PointageAnomalieResponse, jour: PointageAnomalieJour): boolean {
    return this.deplie === this.cle(result, jour);
  }

  /**
   * Clé i18n à afficher sous un champ, ou `null` s'il n'y a rien à signaler. `rechercher()`
   * appelle `markAllAsTouched()` au clic sur "Rechercher" : les erreurs apparaissent donc dès ce
   * clic, sans avoir à toucher/quitter chaque champ un par un au préalable.
   */
  champErreur(nomChamp: string): string | null {
    const control = this.form.get(nomChamp);
    if (!control?.touched || !control.errors) return null;
    if (control.errors['required']) return 'ANOMALIES.FIELD_REQUIRED';
    if (control.errors['pattern']) return nomChamp.startsWith('date') ? 'ANOMALIES.FIELD_INVALID_DATE' : 'ANOMALIES.FIELD_INVALID_TIME';
    return null;
  }

  /**
   * Exporte tous les résultats de la recherche, pas seulement la page actuellement affichée dans
   * le tableau, une ligne par journée en anomalie (pas par badgeage brut) : les heures de
   * badgeage de la journée sont regroupées dans une seule colonne, séparées par
   * `SEPARATEUR_ENTREES` plutôt qu'une colonne par badgeage (leur nombre varie d'une journée à
   * l'autre). Les résultats sont déjà en mémoire côté client, pas d'appel serveur pour ça, juste
   * la construction du fichier et son téléchargement. Mêmes lignes, dans le même ordre (date
   * décroissante, tous salariés confondus), que le tableau affiché à l'écran.
   */
  telechargerCsv(): void {
    const entetes = ['RESULT_EMPLOYEE', 'RESULT_COL_DATE', 'RESULT_COL_ENTRIES']
      .map(cle => this.translate.instant(`ANOMALIES.${cle}`));

    const lignes = this.lignes.map(({ result, jour }) => [
      `${result.employee.prenom} ${result.employee.nom} (${result.employee.matricule})`,
      new Date(jour.date).toLocaleDateString('fr-FR'),
      jour.pointages
        .map(pointage => new Date(pointage.dateHeure).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }))
        .join(PointageAnomaliesComponent.SEPARATEUR_ENTREES)
    ]);

    telechargerCsv(`anomalies-pointage-${horodatageLocal(new Date())}.csv`, entetes, lignes);
  }
}
