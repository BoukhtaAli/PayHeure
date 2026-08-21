import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Subject, forkJoin } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, takeUntil } from 'rxjs/operators';
import { Employee } from '../../models/Employee';
import { EmployeeSearchPeriode, EmployeeService } from '../../services/employee.service';

/**
 * Recherche de salariés par matricule/nom/prénom, avec sélection multiple dans les résultats.
 * La sélection est conservée dans une `Map` (par id) plutôt qu'un tableau simple : elle doit
 * survivre aux changements de page, où le salarié coché n'est plus forcément affiché. Ne charge
 * jamais les pointages elle-même : elle se contente d'émettre la liste des salariés choisis, à
 * charge du parent (écran de calcul de paie) de poursuivre.
 *
 * `initialSelection`/`initialQuery` permettent au parent de restaurer une recherche précédente
 * (ex. retour depuis l'écran de détail) sans que ce composant ait à connaître d'où vient cet état.
 *
 * `requirePeriode` + `calculPeriode` : sur l'écran de calcul de paie, choisir une période (voir
 * paie-calcul.component.ts) est un préalable à la liste, qui ne montre alors que les salariés
 * ayant au moins un pointage dedans — rien ne s'affiche tant qu'aucune période valide n'est
 * fournie. Laissé à `false` par défaut pour les usages sans lien avec une période, comme l'ajout
 * d'un pointage : la liste de tous les salariés actifs s'affiche alors dès le chargement.
 */
@Component({
  selector: 'app-employee-search',
  templateUrl: './employee-search.component.html',
  styleUrls: ['./employee-search.component.css']
})
export class EmployeeSearchComponent implements OnInit, OnChanges, OnDestroy {

  @Input() initialSelection: Employee[] = [];
  @Input() initialQuery = '';

  /** Une seule sélection à la fois : en choisir un nouveau désélectionne le précédent. */
  @Input() singleSelection = false;

  /** Voir le commentaire de classe ci-dessus. */
  @Input() requirePeriode = false;

  /** Voir le commentaire de classe ci-dessus. */
  @Input() calculPeriode: EmployeeSearchPeriode | null = null;

  @Output() readonly employeesSelected = new EventEmitter<Employee[]>();
  @Output() readonly queryChanged = new EventEmitter<string>();

  readonly searchControl = new FormControl('', { nonNullable: true });

  employees: Employee[] = [];
  page = 0;
  totalPages = 0;
  searched = false;

  private readonly selected = new Map<number, Employee>();

  private readonly destroy$ = new Subject<void>();

  /**
   * `false` jusqu'à la fin de `ngOnInit`. Angular appelle `ngOnChanges` une première fois avant
   * `ngOnInit` (pour toute valeur déjà liée dans le template parent, y compris `null`) : sans
   * cette garde, un retour depuis l'écran de détail avec une période déjà choisie déclencherait
   * cette recherche initiale *avant* que `searchControl`/`selected` n'aient été initialisés avec
   * `initialQuery`/`initialSelection` dans `ngOnInit` — la première page recherchée ignorerait
   * alors le texte recherché précédemment. `ngOnInit` s'en charge donc lui-même une fois prêt.
   */
  private initialized = false;

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
      // Sans période requise, la recherche texte s'applique tout de suite ; avec période requise,
      // elle reste sans effet tant qu'aucune période valide n'a été choisie (voir peutAfficherListe).
      if (this.peutAfficherListe()) {
        this.search(0);
      }
    });

    this.initialized = true;

    // Sans période requise (usages hors calcul de paie), la liste se charge dès l'arrivée sur cet
    // écran, pour ne pas le laisser vide. Avec période requise, ne charge que si une période est
    // déjà connue (retour depuis l'écran de détail) ; sinon `ngOnChanges` s'en charge dès qu'une
    // période valide arrive (voir son commentaire) — pas ici, pour ne pas afficher tous les
    // salariés un court instant avant que la période choisie ne les filtre.
    if (!this.requirePeriode) {
      this.search(0);
    } else if (this.calculPeriode) {
      this.search(0);
      this.revaliderSelection(this.calculPeriode);
    }
  }

  /**
   * Avec `requirePeriode`, la liste ne se (re)charge que si `calculPeriode` est valide : tant
   * qu'aucune période n'est choisie, l'écran affiche un état vide plutôt que tous les salariés
   * (voir le template). Un salarié déjà sélectionné (chip) est revérifié à chaque changement de
   * période : s'il n'a plus de pointage dedans, il est automatiquement désélectionné (voir
   * `revaliderSelection`) — sans quoi il resterait coché, et donc inclus dans le calcul, sans plus
   * apparaître dans la liste. `initialized` : voir son commentaire ; le tout premier changement,
   * antérieur à `ngOnInit`, est entièrement pris en charge par celui-ci.
   */
  ngOnChanges(changes: SimpleChanges): void {
    if (!this.initialized || !this.requirePeriode || !changes['calculPeriode']) {
      return;
    }
    if (!this.calculPeriode) {
      this.employees = [];
      this.page = 0;
      this.totalPages = 0;
      this.searched = false;
      return;
    }
    this.search(0);
    this.revaliderSelection(this.calculPeriode);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** `false` seulement quand une période est requise mais pas encore choisie ; voir ngOnInit. */
  private peutAfficherListe(): boolean {
    return !this.requirePeriode || !!this.calculPeriode;
  }

  search(page: number): void {
    this.employeeService.search(this.searchControl.value, page, undefined, this.calculPeriode ?? undefined).subscribe(result => {
      this.employees = result.content;
      this.page = result.page;
      this.totalPages = result.totalPages;
      this.searched = true;
    });
  }

  /**
   * Retire de la sélection les salariés qui n'ont plus de pointage dans `periode`. On revérifie
   * chacun individuellement par son matricule plutôt que de se fier à la liste `employees`
   * actuellement affichée : un salarié absent de la page courante peut très bien rester valide,
   * simplement présent sur une autre page — seul un appel dédié permet de trancher.
   */
  private revaliderSelection(periode: EmployeeSearchPeriode): void {
    if (this.selected.size === 0) {
      return;
    }

    const verifications = this.selectedEmployees.map(employee =>
      this.employeeService.search(employee.matricule, 0, 5, periode).pipe(
        map(result => ({ employee, present: result.content.some(e => e.id === employee.id) }))
      )
    );

    forkJoin(verifications).pipe(takeUntil(this.destroy$)).subscribe(resultats => {
      let modifie = false;
      for (const { employee, present } of resultats) {
        if (!present) {
          this.selected.delete(employee.id);
          modifie = true;
        }
      }
      if (modifie) {
        this.employeesSelected.emit(this.selectedEmployees);
      }
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
}
