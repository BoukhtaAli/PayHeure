import { AfterViewInit, Component, ElementRef, NgZone, OnInit, ViewChild } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Options } from 'flatpickr/dist/types/options';
import { forkJoin } from 'rxjs';
import { first } from 'rxjs/operators';
import { Employee } from '../../models/Employee';
import { PaieCalculResponse } from '../../models/PaieCalcul';
import { EmployeeSearchPeriode } from '../../services/employee.service';
import { PaieResultsService } from '../../services/paie-results.service';
import { PaieService } from '../../services/paie.service';
import { DATE_PATTERN, HEURE_PATTERN, fromIsoDateTime, toIsoDateTime } from '../../utils/date-format';
import { BreadcrumbItem } from '../breadcrumb/breadcrumb.component';

/** Échappe un champ CSV : entre guillemets, guillemets internes doublés (RFC 4180). */
function champCsv(valeur: string): string {
  return `"${valeur.replace(/"/g, '""')}"`;
}

/**
 * `yyyy-MM-dd-HH-mm-ss` en heure locale du navigateur — pas `Date.toISOString()`, qui est
 * toujours en UTC et afficherait donc une heure différente de celle de l'utilisateur.
 */
function horodatageLocal(date: Date): string {
  const deuxChiffres = (n: number) => n.toString().padStart(2, '0');
  return [date.getFullYear(), deuxChiffres(date.getMonth() + 1), deuxChiffres(date.getDate()),
    deuxChiffres(date.getHours()), deuxChiffres(date.getMinutes()), deuxChiffres(date.getSeconds())]
    .join('-');
}

/**
 * La comparaison se fait en chaîne (pas `new Date(...)`) : une fois convertie en
 * `yyyy-MM-ddTHH:mm`, zéro-paddée, l'ordre lexical suit exactement l'ordre chronologique, sans
 * les pièges de fuseau horaire d'un parsing `Date`.
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
 * Écran de calcul de paie : recherche d'un ou plusieurs salariés, saisie de la période et du
 * taux horaire (jamais stocké côté serveur), puis récupération des pointages et calcul du
 * montant dû pour chacun des salariés sélectionnés.
 */
@Component({
  selector: 'app-paie-calcul',
  templateUrl: './paie-calcul.component.html',
  styleUrls: ['./paie-calcul.component.css']
})
export class PaieCalculComponent implements OnInit, AfterViewInit {

  readonly breadcrumbItems: BreadcrumbItem[] = [
    { labelKey: 'NAV.HOME', link: ['/home'] },
    { labelKey: 'NAV.PAIE' }
  ];

  /**
   * `d-m-Y` est la syntaxe de formatage propre à flatpickr (équivalent de notre `dd-MM-yyyy`).
   * Pas `readonly` : `minDate`/`maxDate` sont recalculés et réassignés (nouvelle référence, pas
   * mutation) dans `onSearchPeriodeChanged`, ce qui déclenche `ngOnChanges` sur la directive
   * flatpickr — voir flatpickr.directive.ts.
   */
  dateOptions: Partial<Options> = {
    dateFormat: 'd-m-Y'
  };

  /** `noCalendar` + `time_24hr` : horloge seule, toujours en 24h, jamais de calendrier ni d'AM/PM. */
  readonly heureOptions: Partial<Options> = {
    enableTime: true,
    noCalendar: true,
    dateFormat: 'H:i',
    time_24hr: true
  };

  selectedEmployees: Employee[] = [];
  results: PaieCalculResponse[] = [];
  errorMessage: string | null = null;

  /** Recherche à restaurer dans <app-employee-search> ; voir ngOnInit. */
  initialSearchQuery = '';

  /**
   * Période choisie dans le filtre "Période travaillée" de la recherche de salariés (voir
   * onSearchPeriodeChanged). Contraint le calendrier (minDate/maxDate) et sert de garde-fou dans
   * `periodeDansRechercheValidator` : la période de calcul ne doit pas dépasser la période sur
   * laquelle les salariés ont été présélectionnés.
   */
  searchPeriode: EmployeeSearchPeriode | null = null;

  /**
   * `true` seulement quand on arrive via le bouton "Retour" de l'écran de détail (voir
   * ngOnInit) : sert à ngAfterViewInit pour placer la page sur les résultats déjà calculés
   * plutôt qu'en haut du formulaire, sans quoi l'utilisateur perdrait le fil en revenant.
   */
  private returningFromDetail = false;

  /** Cible du scroll dans `ngAfterViewInit` ; voir son commentaire. */
  @ViewChild('resultsPanel') private resultsPanel?: ElementRef<HTMLElement>;

  readonly form: FormGroup = this.fb.group({
    dateDebut: ['', [Validators.required, Validators.pattern(DATE_PATTERN)]],
    heureDebut: ['', [Validators.required, Validators.pattern(HEURE_PATTERN)]],
    dateFin: ['', [Validators.required, Validators.pattern(DATE_PATTERN)]],
    heureFin: ['', [Validators.required, Validators.pattern(HEURE_PATTERN)]],
    tauxHoraire: [null, [Validators.required, Validators.min(0.01)]]
  }, { validators: [periodValidator, group => this.periodeDansRechercheValidator(group)] });

  constructor(
    private readonly fb: FormBuilder,
    private readonly paieService: PaieService,
    private readonly paieResultsService: PaieResultsService,
    private readonly translate: TranslateService,
    private readonly router: Router,
    private readonly ngZone: NgZone
  ) {}

  ngOnInit(): void {
    // Ne restaure l'état du dernier calcul que si on arrive via le bouton "Retour" de l'écran de
    // détail (voir PaieDetailComponent.retourVersCalcul) : toute autre façon d'arriver ici
    // (accueil, barre de navigation, fil d'Ariane, URL directe) repart d'un formulaire vide, pas
    // de la dernière recherche en date. Flag à usage unique, consommé ici.
    if (this.paieResultsService.restoreOnNextLoad) {
      this.paieResultsService.restoreOnNextLoad = false;
      this.selectedEmployees = this.paieResultsService.selectedEmployees;
      this.results = this.paieResultsService.results;
      this.initialSearchQuery = this.paieResultsService.searchQuery;
      if (this.paieResultsService.formValue) {
        this.form.patchValue(this.paieResultsService.formValue);
      }
      this.returningFromDetail = true;
    } else {
      this.paieResultsService.reset();
    }
  }

  /**
   * Au retour depuis l'écran de détail, la page doit s'ouvrir sur le tableau des résultats, pas
   * sur le formulaire en haut de page. Défilement instantané (pas `smooth`) : contrairement au
   * clic sur "Calculer" (voir `scrollToResults`), ce n'est pas une action déclenchée par
   * l'utilisateur pendant qu'il regarde la page, mais un repositionnement au chargement.
   */
  ngAfterViewInit(): void {
    if (this.returningFromDetail) {
      this.returningFromDetail = false;
      this.scrollToResults('auto');
    }
  }

  /**
   * Cible directement `resultsPanel` (scrollIntoView) plutôt que de calculer la hauteur totale du
   * document et scroller tout en bas : `<app-employee-search>` relance sa recherche dans son
   * propre `ngOnInit` (voir employee-search.component.ts, appel HTTP asynchrone), et si d'autres
   * éléments sous le tableau (pagination, footer...) changent de hauteur après coup, un scroll
   * basé sur la hauteur totale de la page retomberait trop court ou trop loin — cibler l'élément
   * lui-même n'a pas ce problème.
   *
   * `NgZone.onStable` attend que les tâches en cours (dont l'appel HTTP ci-dessus, ou celui de
   * `calculer()`) soient terminées avant de mesurer la position du tableau ; `isStable` évite
   * d'attendre indéfiniment si tout était déjà résolu avant qu'on ne s'y abonne (l'événement ne
   * se redéclenche que sur une prochaine transition instable → stable, qui pourrait ne jamais
   * survenir). Le `requestAnimationFrame` laisse ensuite le navigateur peindre la mise en page
   * finale avant de scroller.
   */
  private scrollToResults(behavior: ScrollBehavior): void {
    const scroll = () => requestAnimationFrame(() => {
      this.resultsPanel?.nativeElement.scrollIntoView({ block: 'end', behavior });
    });
    if (this.ngZone.isStable) {
      scroll();
    } else {
      this.ngZone.onStable.pipe(first()).subscribe(scroll);
    }
  }

  onEmployeesSelected(employees: Employee[]): void {
    this.selectedEmployees = employees;
    this.paieResultsService.selectedEmployees = employees;
    this.results = [];
    this.errorMessage = null;
  }

  onQueryChanged(query: string): void {
    this.paieResultsService.searchQuery = query;
  }

  /**
   * Contraint le calendrier de la période de calcul à ne pas dépasser la période de recherche
   * (nouvelle borne min/max ; l'utilisateur peut toujours dépasser en tapant au clavier, d'où
   * `periodeDansRechercheValidator` en garde-fou). `null` quand le filtre est vidé : plus aucune
   * contrainte, comportement d'avant cette fonctionnalité.
   */
  onSearchPeriodeChanged(periode: EmployeeSearchPeriode | null): void {
    this.searchPeriode = periode;
    this.dateOptions = {
      dateFormat: 'd-m-Y',
      minDate: periode ? new Date(periode.dateDebut) : undefined,
      maxDate: periode ? new Date(periode.dateFin) : undefined
    };
    this.form.updateValueAndValidity();
  }

  calculer(): void {
    if (this.selectedEmployees.length === 0 || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage = null;
    const { dateDebut, heureDebut, dateFin, heureFin, tauxHoraire } = this.form.value;
    const periode = {
      dateDebut: toIsoDateTime(dateDebut, heureDebut),
      dateFin: toIsoDateTime(dateFin, heureFin),
      tauxHoraire
    };

    // Même période et même tarif pour tout le monde : un appel par salarié sélectionné, en
    // parallèle plutôt qu'en série, tous attendus avant d'afficher les résultats.
    const calculs = this.selectedEmployees.map(employee =>
      this.paieService.calculer({ employeeId: employee.id, ...periode }));

    // Filtres utilisés pour ce calcul, à restaurer si on revient sur cet écran (voir ngOnInit).
    this.paieResultsService.formValue = this.form.value;

    forkJoin(calculs).subscribe({
      next: responses => {
        this.results = responses;
        // Partagé avec l'écran de détail (voir PaieResultsService) : évite un rappel backend pour
        // un calcul déjà fait, puisqu'il n'y a de toute façon rien à récupérer par id côté serveur.
        this.paieResultsService.results = responses;
        // `smooth`, contrairement au retour depuis l'écran de détail : ici l'utilisateur regarde
        // déjà la page au moment où le tableau apparaît, un saut instantané serait déroutant.
        if (responses.length > 0) {
          this.scrollToResults('smooth');
        }
      },
      error: error => {
        this.results = [];
        // Le backend ne répond qu'en français (voir GlobalExceptionHandler) et ce message n'est
        // pas traduit, contrairement au reste de l'écran. Le seul cas facilement prévisible
        // (période invalide) est donc intercepté avant l'appel par `periodValidator` ; ce message
        // générique ne sert plus que pour les erreurs réellement inattendues côté serveur.
        this.errorMessage = error?.error?.message ?? this.translate.instant('PAIE.CALCUL_ERROR');
      }
    });
  }

  voirDetail(result: PaieCalculResponse): void {
    this.router.navigate(['/paie/detail', result.employee.id]);
  }

  /**
   * Clé i18n à afficher sous un champ, ou `null` s'il n'y a rien à signaler. `calculer()` appelle
   * `markAllAsTouched()` au clic sur "Calculer" : les erreurs apparaissent donc dès ce clic, sans
   * avoir à toucher/quitter chaque champ un par un au préalable.
   */
  champErreur(nomChamp: string): string | null {
    const control = this.form.get(nomChamp);
    if (!control?.touched || !control.errors) return null;
    if (control.errors['required']) return 'PAIE.FIELD_REQUIRED';
    if (control.errors['pattern']) return nomChamp.startsWith('date') ? 'PAIE.FIELD_INVALID_DATE' : 'PAIE.FIELD_INVALID_TIME';
    if (control.errors['min']) return 'PAIE.FIELD_INVALID_RATE';
    return null;
  }

  /**
   * La période de calcul doit rester à l'intérieur de `searchPeriode` (bornes incluses) quand
   * elle est renseignée. Les deux bornes de `searchPeriode` ont les secondes (`T00:00:00`,
   * `T23:59:59`) alors que `toIsoDateTime` n'en a pas (`heureDebut`/`heureFin` au format `HH:mm`) :
   * on tronque à 16 caractères des deux côtés avant de comparer, sinon une période de calcul
   * commençant *exactement* à la borne de recherche serait à tort jugée hors limites (chaîne plus
   * courte que la borne mais par ailleurs identique = "plus petite" lexicographiquement).
   */
  private periodeDansRechercheValidator(group: AbstractControl): ValidationErrors | null {
    if (!this.searchPeriode) return null;

    const { dateDebut, heureDebut, dateFin, heureFin } = group.value;
    const champsValides = DATE_PATTERN.test(dateDebut) && HEURE_PATTERN.test(heureDebut)
      && DATE_PATTERN.test(dateFin) && HEURE_PATTERN.test(heureFin);
    if (!champsValides) return null;

    const debut = toIsoDateTime(dateDebut, heureDebut);
    const fin = toIsoDateTime(dateFin, heureFin);
    const limiteDebut = this.searchPeriode.dateDebut.slice(0, 16);
    const limiteFin = this.searchPeriode.dateFin.slice(0, 16);

    return (debut < limiteDebut || fin > limiteFin) ? { outsideSearchPeriode: true } : null;
  }

  /**
   * Exporte la liste de résultats affichée, telle quelle (mêmes colonnes que le tableau). Les
   * résultats sont déjà en mémoire côté client (voir `calculer()`) : pas d'appel serveur pour
   * ça, juste la construction du fichier et son téléchargement.
   */
  telechargerCsv(): void {
    const devise = this.translate.instant('PAIE.CURRENCY');
    const parHeure = this.translate.instant('PAIE.PER_HOUR');
    const entetes = ['RESULT_EMPLOYEE', 'RESULT_PERIOD', 'RESULT_RATE', 'RESULT_DURATION', 'RESULT_TOTAL']
      .map(cle => this.translate.instant(`PAIE.${cle}`));

    const lignes = this.results.map(result => [
      `${result.employee.prenom} ${result.employee.nom} (${result.employee.matricule})`,
      `${fromIsoDateTime(result.dateDebut)} - ${fromIsoDateTime(result.dateFin)}`,
      `${result.tauxHoraire.toFixed(2)} ${devise} ${parHeure}`,
      result.totalDureeFormatee,
      `${result.montantTotal.toFixed(2)} ${devise}`
    ]);

    // Séparateur `;` plutôt que `,` : convention des CSV en locale française (Excel FR utilise
    // la virgule comme séparateur décimal, donc `,` comme délimiteur de colonnes casse l'import).
    const csv = [entetes, ...lignes]
      .map(ligne => ligne.map(champCsv).join(';'))
      .join('\r\n');

    // BOM UTF-8 en préfixe (U+FEFF, code explicite pour éviter un caractère invisible dans la
    // source) : sans lui, Excel interprète les accents (salarié, prénom...) de travers.
    const bom = String.fromCharCode(0xFEFF);
    const blob = new Blob([bom + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const lien = document.createElement('a');
    lien.href = url;
    // Minutes et secondes incluses (pas seulement la date) : deux exports le même jour ne
    // s'écrasent pas l'un l'autre.
    lien.download = `paie-${horodatageLocal(new Date())}.csv`;
    lien.click();
    URL.revokeObjectURL(url);
  }
}
