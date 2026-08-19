import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { Employee } from '../../models/Employee';
import { EmployeeService } from '../../services/employee.service';

/**
 * Recherche de salarié par matricule/nom/prénom, avec sélection dans les résultats. Ne charge
 * jamais ses pointages elle-même : elle se contente d'émettre le salarié choisi, à charge du
 * parent (écran de calcul de paie) de poursuivre.
 */
@Component({
  selector: 'app-employee-search',
  templateUrl: './employee-search.component.html',
  styleUrls: ['./employee-search.component.css']
})
export class EmployeeSearchComponent implements OnInit, OnDestroy {

  @Output() readonly employeeSelected = new EventEmitter<Employee>();

  readonly searchControl = new FormControl('', { nonNullable: true });

  employees: Employee[] = [];
  page = 0;
  totalPages = 0;
  selectedEmployeeId: number | null = null;
  searched = false;

  private readonly destroy$ = new Subject<void>();

  constructor(private readonly employeeService: EmployeeService) {}

  ngOnInit(): void {
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => this.search(0));

    // Liste initiale (tous les salariés actifs), pour ne pas laisser l'écran vide.
    this.search(0);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  search(page: number): void {
    this.employeeService.search(this.searchControl.value, page).subscribe(result => {
      this.employees = result.content;
      this.page = result.page;
      this.totalPages = result.totalPages;
      this.searched = true;
    });
  }

  select(employee: Employee): void {
    this.selectedEmployeeId = employee.id;
    this.employeeSelected.emit(employee);
  }
}
