import { Component, HostListener } from '@angular/core';

/** Bouton flottant "retour en haut" repris du projet catalog, affiché sur toutes les pages (voir app.component.html). */
@Component({
  selector: 'app-go-to-top',
  templateUrl: './go-to-top.component.html',
  styleUrls: ['./go-to-top.component.css']
})
export class GoToTopComponent {
  isVisible: boolean = true;

  @HostListener('window:scroll')
  onWindowScroll(): void {
    this.isVisible = window.scrollY > 200;
  }

  goToTop(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
