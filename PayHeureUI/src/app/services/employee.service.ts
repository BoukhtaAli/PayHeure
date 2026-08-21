import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Employee } from '../models/Employee';
import { PageResponse } from '../models/PageResponse';
import { API_BASE_URL } from '../config/api.config';

/** Taille de page par défaut de la recherche de salariés. */
export const DEFAULT_PAGE_SIZE = 5;

/** Restreint la recherche aux salariés ayant au moins un pointage entre ces deux bornes ISO. */
export interface EmployeeSearchPeriode {
  dateDebut: string;
  dateFin: string;
}

@Injectable({
  providedIn: 'root'
})
export class EmployeeService {

  private readonly apiUrl = `${API_BASE_URL}/employees`;

  constructor(private http: HttpClient) {}

  /**
   * Les salariés dont le matricule, le nom ou le prénom contient le texte recherché, restreints
   * en plus à ceux ayant au moins un pointage pendant `periode` si elle est fournie.
   */
  search(
    query: string,
    page: number = 0,
    size: number = DEFAULT_PAGE_SIZE,
    periode?: EmployeeSearchPeriode
  ): Observable<PageResponse<Employee>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (query.trim() !== '') {
      params = params.set('query', query.trim());
    }
    if (periode) {
      params = params.set('dateDebut', periode.dateDebut).set('dateFin', periode.dateFin);
    }
    return this.http.get<PageResponse<Employee>>(this.apiUrl, { params });
  }

  getById(id: number): Observable<Employee> {
    return this.http.get<Employee>(`${this.apiUrl}/${id}`);
  }
}
