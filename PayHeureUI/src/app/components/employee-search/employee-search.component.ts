import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Options } from 'flatpickr/dist/types/options';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { Employee } from '../../models/Employee';
import { EmployeeSearchPeriode, EmployeeService } from '../../services/employee.service';
import { DATE_PATTERN, toIsoDate } from '../../utils/date-format';

/** Sépare les deux bornes dans le champ période ; voir `periodeOptions`. */
const SEPARATEUR_PERIODE = ' → ';

/**
 * Recherche de salariés par matricule/nom/prénom, avec sélection multiple dans les résultats.
 * La sélection est conservée dans une `Map` (par id) plutôt qu'un tableau simple : elle doit
 * survivre aux changements de page, où le salarié coché n'est plus forcément affiché. Ne charge
 * jamais les pointages elle-même : elle se contente d'émettre la liste des salariés choisis, à
 * charge du parent (écran de calcul de paie) de poursuivre.
 *
 * Le filtre de période (calendrier flatpickr en mode plage) se combine avec la recherche
 * textuelle : ne renvoie que les salariés ayant au moins un pointage entre les deux dates
 * choisies, matricule/nom/prénom en plus si renseigné.
 *
 * `initialSelection`/`initialQuery` permettent au parent de restaurer une recherche précédente
 * (ex. retour depuis l'écran de détail) sans que ce composant ait à connaître d'où vient cet état.
 *
 * `periodeChanged` permet au parent de contraindre la période de calcul de paie à ne pas dépasser
 * cette période de recherche (voir paie-calcul.component.ts) : sans ça, rien n'empêchait de
 * chercher des salariés ayant travaillé du 2 au 20 août puis de calculer leur paie du 1er au 31.
 */
@Component({
  selector: 'app-employee-search',
  templateUrl: './employee-search.component.html',
  styleUrls: ['./employee-search.component.css']
})
export class EmployeeSearchComponent implements OnInit, OnDestroy {

  @Input() initialSelection: Employee[] = [];
  @Input() initialQuery = '';

  /** Une seule sélection à la fois : en choisir un nouveau désélectionne le précédent. */
  @Input() singleSelection = false;

  /** Masque le filtre "Période travaillée", inutile pour un usage sans lien avec le calcul de paie. */
  @Input() showPeriodeFilter = true;

  @Output() readonly employeesSelected = new EventEmitter<Employee[]>();
  @Output() readonly queryChanged = new EventEmitter<string>();
  @Output() readonly periodeChanged = new EventEmitter<EmployeeSearchPeriode | null>();

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly periodeControl = new FormControl('', { nonNullable: true });

  /**
   * Calendrier plutôt qu'un <input type="date"> : voir flatpickr.directive.ts.
   * En mode "range", flatpickr sépare les deux dates avec `locale.rangeSeparator`, pas
   * `conjunction` (qui ne s'applique qu'en mode "multiple") — piège vérifié dans son code source
   * (flatpickr.js, fonction getDateStr) après avoir constaté que le filtre période ne se
   * déclenchait jamais : `conjunction` était silencieusement ignoré.
   */
  readonly periodeOptions: Partial<Options> = {
    mode: 'range',
    dateFormat: 'd-m-Y',
    locale: { rangeSeparator: SEPARATEUR_PERIODE }
  };

  employees: Employee[] = [];
  page = 0;
  totalPages = 0;
  searched = false;

  private readonly selected = new Map<number, Employee>();

  private readonly destroy$ = new Subject<void>();

  constructor(private readonly employeeService: EmployeeService) {}

  ngOnInit(): void {
    for (const employee of this.initialSelection) {
      this.selected.set(employee.id, employee);
    }
    if (this.initialQuery) {
      this.searchControl.setValue(this.initialQuery, { emitEvent: false });
    }

    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(query => {
      this.queryChanged.emit(query);
      this.search(0);
    });

    // Pas de debounce ici : la valeur ne change qu'à la fermeture du calendrier (voir flatpickr's
    // onChange), pas à chaque frappe comme le texte libre.
    this.periodeControl.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.periodeChanged.emit(this.periode() ?? null);
      this.search(0);
    });

    // Liste initiale (tous les salariés actifs, ou déjà filtrée par initialQuery), pour ne pas
    // laisser l'écran vide.
    this.search(0);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  search(page: number): void {
    this.employeeService.search(this.searchControl.value, page, undefined, this.periode()).subscribe(result => {
      this.employees = result.content;
      this.page = result.page;
      this.totalPages = result.totalPages;
      this.searched = true;
    });
  }

  get selectedEmployees(): Employee[] {
    return Array.from(this.selected.values());
  }

  isSelected(employee: Employee): boolean {
    return this.selected.has(employee.id);
  }

  toggle(employee: Employee): void {
    if (this.selected.has(employee.id)) {
      this.selected.delete(employee.id);
    } else {
      if (this.singleSelection) {
        this.selected.clear();
      }
      this.selected.set(employee.id, employee);
    }
    this.employeesSelected.emit(this.selectedEmployees);
  }

  remove(employee: Employee): void {
    this.selected.delete(employee.id);
    this.employeesSelected.emit(this.selectedEmployees);
  }

  /**
   * `undefined` tant que les deux bornes n'ont pas été choisies (une seule date sélectionnée =
   * pas encore un filtre exploitable). Bornées à la journée entière : ce filtre porte sur des
   * jours, pas des heures précises, contrairement à la période du calcul de paie.
   */
  private periode(): EmployeeSearchPeriode | undefined {
    const [debut, fin] = this.periodeControl.value.split(SEPARATEUR_PERIODE);
    if (!debut || !fin || !DATE_PATTERN.test(debut) || !DATE_PATTERN.test(fin)) {
      return undefined;
    }
    return {
      dateDebut: `${toIsoDate(debut)}T00:00:00`,
      dateFin: `${toIsoDate(fin)}T23:59:59`
    };
  }
}
