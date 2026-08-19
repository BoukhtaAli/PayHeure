import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaieCalculRequest, PaieCalculResponse } from '../models/PaieCalcul';
import { API_BASE_URL } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class PaieService {

  private readonly apiUrl = `${API_BASE_URL}/paie`;

  constructor(private http: HttpClient) {}

  /**
   * Récupère les pointages du salarié sur la période demandée et calcule le montant dû, au
   * taux horaire fourni (jamais transmis ailleurs qu'à cet appel).
   */
  calculer(request: PaieCalculRequest): Observable<PaieCalculResponse> {
    return this.http.post<PaieCalculResponse>(`${this.apiUrl}/calcul`, request);
  }
}
