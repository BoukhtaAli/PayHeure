import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PointageAnomalieRequest, PointageAnomalieResponse } from '../models/PointageAnomalie';
import { API_BASE_URL } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class PointageAnomalieService {

  private readonly apiUrl = `${API_BASE_URL}/pointages`;

  constructor(private http: HttpClient) {}

  /** Salariés ayant, sur la période demandée, au moins un badgeage sans sortie correspondante. */
  lister(request: PointageAnomalieRequest): Observable<PointageAnomalieResponse[]> {
    return this.http.post<PointageAnomalieResponse[]>(`${this.apiUrl}/anomalies`, request);
  }
}
