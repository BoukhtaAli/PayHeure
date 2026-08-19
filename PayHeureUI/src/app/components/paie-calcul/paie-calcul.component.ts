import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { Options } from 'flatpickr/dist/types/options';
import { Employee } from '../../models/Employee';
import { PaieCalculResponse } from '../../models/PaieCalcul';
import { PaieService } from '../../services/paie.service';
import { BreadcrumbItem } from '../breadcrumb/breadcrumb.component';

/**
 * Format imposé aux champs date/heure, en secours de la config flatpickr (voir `dateOptions` /
 * `heureOptions`) : au cas où l'utilisateur tape une valeur à la main plutôt que de passer par le
 * calendrier/l'horloge dessinés par la librairie. `HH:mm` sur 24h (00-23), jamais AM/PM.
 */
const HEURE_PATTERN = /^([01]\d|2[0-3]):[0-5]\d$/;

/** Format `dd-MM-yyyy` fixe, jamais `MM/dd/yyyy` — voir le commentaire de `HEURE_PATTERN`. */
const DATE_PATTERN = /^(0[1-9]|[12]\d|3[01])-(0[1-9]|1[0-2])-\d{4}$/;

/** Convertit une date saisie `dd-MM-yyyy` en `yyyy-MM-dd` (ISO, attendu par le backend). */
function toIsoDate(dateSaisie: string): string {
  const [jour, mois, annee] = dateSaisie.split('-');
  return `${annee}-${mois}-${jour}`;
}

/** Combine une date (`dd-MM-yyyy`) et une heure (`HH:mm`) en `LocalDateTime` ISO. */
function toIsoDateTime(date: string, heure: string): string {
  return `${toIsoDate(date)}T${heure}`;
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
 * Écran de calcul de paie : recherche du salarié, saisie de la période et du taux horaire
 * (jamais stocké côté serveur), puis récupération des pointages et calcul du montant dû.
 */
@Component({
  selector: 'app-paie-calcul',
  templateUrl: './paie-calcul.component.html',
  styleUrls: ['./paie-calcul.component.css']
})
export class PaieCalculComponent {

  readonly breadcrumbItems: BreadcrumbItem[] = [
    { labelKey: 'NAV.HOME', link: ['/home'] },
    { labelKey: 'NAV.PAIE' }
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

  selectedEmployee: Employee | null = null;
  result: PaieCalculResponse | null = null;
  errorMessage: string | null = null;

  readonly form: FormGroup = this.fb.group({
    dateDebut: ['', [Validators.required, Validators.pattern(DATE_PATTERN)]],
    heureDebut: ['', [Validators.required, Validators.pattern(HEURE_PATTERN)]],
    dateFin: ['', [Validators.required, Validators.pattern(DATE_PATTERN)]],
    heureFin: ['', [Validators.required, Validators.pattern(HEURE_PATTERN)]],
    tauxHoraire: [null, [Validators.required, Validators.min(0.01)]]
  }, { validators: periodValidator });

  constructor(
    private readonly fb: FormBuilder,
    private readonly paieService: PaieService,
    private readonly translate: TranslateService
  ) {}

  onEmployeeSelected(employee: Employee): void {
    this.selectedEmployee = employee;
    this.result = null;
    this.errorMessage = null;
  }

  calculer(): void {
    if (!this.selectedEmployee || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage = null;
    const { dateDebut, heureDebut, dateFin, heureFin, tauxHoraire } = this.form.value;

    this.paieService.calculer({
      employeeId: this.selectedEmployee.id,
      dateDebut: toIsoDateTime(dateDebut, heureDebut),
      dateFin: toIsoDateTime(dateFin, heureFin),
      tauxHoraire
    }).subscribe({
      next: response => this.result = response,
      error: error => {
        this.result = null;
        // Le backend ne répond qu'en français (voir GlobalExceptionHandler) et ce message n'est
        // pas traduit, contrairement au reste de l'écran. Le seul cas facilement prévisible
        // (période invalide) est donc intercepté avant l'appel par `periodValidator` ; ce message
        // générique ne sert plus que pour les erreurs réellement inattendues côté serveur.
        this.errorMessage = error?.error?.message ?? this.translate.instant('PAIE.CALCUL_ERROR');
      }
    });
  }
}
