import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Options } from 'flatpickr/dist/types/options';
import { Employee } from '../../models/Employee';
import { Pointage } from '../../models/Pointage';
import { PointageService } from '../../services/pointage.service';
import { DATE_PATTERN, HEURE_PATTERN, toIsoDateTime } from '../../utils/date-format';
import { BreadcrumbItem } from '../breadcrumb/breadcrumb.component';

/**
 * Écran de saisie manuelle d'un pointage : recherche et sélection d'un salarié (une seule à la
 * fois, voir `singleSelection` sur <app-employee-search>), puis date/heure du badgeage.
 *
 * Le salarié reste sélectionné après l'ajout, et seule l'heure est réinitialisée (la date est
 * conservée) : le cas le plus courant est d'enchaîner plusieurs badgeages du même salarié la même
 * journée (entrée, sortie...). `ajoutes` ne fait que rappeler ce qui a été saisi dans cette
 * session ; il n'existe pas d'endpoint pour relister les pointages bruts d'un salarié (voir
 * PaieController, qui les renvoie uniquement en tant que sous-produit d'un calcul de paie).
 */
@Component({
  selector: 'app-pointage-ajout',
  templateUrl: './pointage-ajout.component.html',
  styleUrls: ['./pointage-ajout.component.css']
})
export class PointageAjoutComponent {

  readonly breadcrumbItems: BreadcrumbItem[] = [
    { labelKey: 'NAV.HOME', link: ['/home'] },
    { labelKey: 'NAV.POINTAGE' }
  ];

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
  ajoutes: Pointage[] = [];
  submitting = false;

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

  readonly form: FormGroup = this.fb.group({
    date: ['', [Validators.required, Validators.pattern(DATE_PATTERN)]],
    heure: ['', [Validators.required, Validators.pattern(HEURE_PATTERN)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly pointageService: PointageService
  ) {}

  /** Changer de salarié repart d'une liste de pointages ajoutés vide : elle est propre à celui affiché. */
  onEmployeeSelected(employees: Employee[]): void {
    const employee = employees[0] ?? null;
    if (employee?.id !== this.selectedEmployee?.id) {
      this.ajoutes = [];
    }
    this.selectedEmployee = employee;
    this.errorMessage = null;
    this.errorMessageKey = null;
  }

  ajouter(): void {
    if (!this.selectedEmployee || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMessage = null;
    this.errorMessageKey = null;
    const { date, heure } = this.form.value;

    this.pointageService.creer({
      employeeId: this.selectedEmployee.id,
      dateHeure: toIsoDateTime(date, heure)
    }).subscribe({
      next: pointage => {
        this.ajoutes.unshift(pointage);
        this.submitting = false;
        // Seule l'heure est vidée : voir le commentaire de classe.
        this.form.patchValue({ heure: '' });
        this.form.get('heure')?.markAsUntouched();
      },
      error: error => {
        this.submitting = false;
        // Le backend ne répond qu'en français (voir GlobalExceptionHandler) et ce message n'est
        // pas traduit ; `errorMessage` (texte déjà résolu) sert seulement à ce cas brut venu du
        // backend, `errorMessageKey` (clé i18n) au message générique, traduit dans le template via
        // le pipe `translate` pour rester à jour si l'utilisateur change de langue ensuite.
        const messageBrut = error?.error?.message;
        this.errorMessage = messageBrut ?? null;
        this.errorMessageKey = messageBrut ? null : 'POINTAGE.SAVE_ERROR';
      }
    });
  }

  /**
   * Clé i18n à afficher sous un champ, ou `null` s'il n'y a rien à signaler. `ajouter()` appelle
   * `markAllAsTouched()` au clic sur "Ajouter" : les erreurs apparaissent donc dès ce clic, sans
   * avoir à toucher/quitter chaque champ un par un au préalable.
   */
  champErreur(nomChamp: string): string | null {
    const control = this.form.get(nomChamp);
    if (!control?.touched || !control.errors) return null;
    if (control.errors['required']) return 'POINTAGE.FIELD_REQUIRED';
    if (control.errors['pattern']) return nomChamp === 'date' ? 'POINTAGE.FIELD_INVALID_DATE' : 'POINTAGE.FIELD_INVALID_TIME';
    return null;
  }
}
