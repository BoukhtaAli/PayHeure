import { Component, Input } from '@angular/core';
import { PaieCalculResponse } from '../../models/PaieCalcul';

/** Affiche le résultat d'un calcul de paie : sessions reconstituées, anomalies et total. */
@Component({
  selector: 'app-pointage-result',
  templateUrl: './pointage-result.component.html',
  styleUrls: ['./pointage-result.component.css']
})
export class PointageResultComponent {
  @Input() result: PaieCalculResponse | null = null;

  get anomalies() {
    return this.result?.sessions.filter(session => session.anomalie) ?? [];
  }

  get validSessions() {
    return this.result?.sessions.filter(session => !session.anomalie) ?? [];
  }
}
