import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PaieCalculResponse } from '../../models/PaieCalcul';
import { PaieResultsService } from '../../services/paie-results.service';
import { BreadcrumbItem } from '../breadcrumb/breadcrumb.component';

/**
 * Détail du calcul de paie d'un salarié : repris depuis {@link PaieResultsService} (résultat déjà
 * calculé sur l'écran précédent), pas re-demandé au backend. `result` reste `null` si on arrive
 * ici directement (lien partagé, rechargement de page) sans être passé par l'écran de calcul.
 */
@Component({
  selector: 'app-paie-detail',
  templateUrl: './paie-detail.component.html',
  styleUrls: ['./paie-detail.component.css']
})
export class PaieDetailComponent implements OnInit {

  result: PaieCalculResponse | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly paieResultsService: PaieResultsService
  ) {}

  ngOnInit(): void {
    const employeeId = Number(this.route.snapshot.paramMap.get('employeeId'));
    this.result = this.paieResultsService.findByEmployeeId(employeeId) ?? null;
  }

  /**
   * Seul ce bouton restaure l'écran de calcul tel qu'il était (recherche, sélection, période,
   * tarif) : le fil d'Ariane et toute autre navigation vers /paie repartent d'un formulaire vide,
   * voir PaieResultsService.restoreOnNextLoad.
   */
  retourVersCalcul(): void {
    this.paieResultsService.restoreOnNextLoad = true;
    this.router.navigate(['/paie']);
  }

  get breadcrumbItems(): BreadcrumbItem[] {
    const items: BreadcrumbItem[] = [
      { labelKey: 'NAV.HOME', link: ['/home'] },
      { labelKey: 'NAV.PAIE', link: ['/paie'] }
    ];
    if (this.result) {
      items.push({ label: `${this.result.employee.prenom} ${this.result.employee.nom}` });
    }
    return items;
  }
}
