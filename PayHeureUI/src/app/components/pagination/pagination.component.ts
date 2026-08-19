import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Pagination générique à ellipses, partagée par tous les écrans consommant un `PageResponse`
 * du backend. Le parent reste responsable du chargement des données ; ce composant se contente
 * de calculer les numéros de page à afficher et d'émettre la page choisie.
 */
@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.component.html',
  styleUrls: ['./pagination.component.css']
})
export class PaginationComponent {

  /** Page courante, 0-indexée (alignée sur le contrat `PageResponse` du backend). */
  @Input() page = 0;
  @Input() totalPages = 0;
  @Input() ariaLabel = '';

  @Output() readonly pageChange = new EventEmitter<number>();

  /** Les numéros de page à afficher, `null` matérialisant une ellipse entre deux groupes. */
  get visiblePages(): (number | null)[] {
    const delta = 2;
    const pages: (number | null)[] = [];

    for (let i = 0; i < this.totalPages; i++) {
      const isEdge = i === 0 || i === this.totalPages - 1;
      const isNearCurrent = Math.abs(i - this.page) <= delta;

      if (isEdge || isNearCurrent) {
        pages.push(i);
      } else if (pages[pages.length - 1] !== null) {
        pages.push(null);
      }
    }
    return pages;
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages || page === this.page) return;
    this.pageChange.emit(page);
  }
}
