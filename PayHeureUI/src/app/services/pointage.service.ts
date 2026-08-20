import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Pointage, PointageCreateRequest } from '../models/Pointage';
import { API_BASE_URL } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class PointageService {

  private readonly apiUrl = `${API_BASE_URL}/pointages`;

  constructor(private http: HttpClient) {}

  /** Ajoute un badgeage pour le salarié désigné dans la requête. */
  creer(request: PointageCreateRequest): Observable<Pointage> {
    return this.http.post<Pointage>(this.apiUrl, request);
  }
}
