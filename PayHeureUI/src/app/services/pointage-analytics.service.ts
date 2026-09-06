import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PointageAnalyticsRequest, PointageAnalyticsResponse } from '../models/PointageAnalytics';
import { API_BASE_URL } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class PointageAnalyticsService {

  private readonly apiUrl = `${API_BASE_URL}/pointages`;

  constructor(private http: HttpClient) {}

  /** Statistiques de complétude des pointages, tous salariés confondus, agrégées par jour/semaine/mois. */
  calculer(request: PointageAnalyticsRequest): Observable<PointageAnalyticsResponse> {
    return this.http.post<PointageAnalyticsResponse>(`${this.apiUrl}/analytics`, request);
  }
}
