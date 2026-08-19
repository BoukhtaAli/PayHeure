import { Injectable } from '@angular/core';
import { Employee } from '../models/Employee';
import { PaieCalculResponse } from '../models/PaieCalcul';

export interface PaieFormValue {
  dateDebut: string;
  heureDebut: string;
  dateFin: string;
  heureFin: string;
  tauxHoraire: number | null;
}

/**
 * Conserve en mémoire l'état du dernier calcul de paie (recherche, salariés sélectionnés,
 * période/tarif saisis, résultats), le temps de la navigation entre l'écran de calcul et le
 * détail d'un salarié. Permet au bouton retour de l'écran de détail de retrouver l'écran de
 * calcul tel qu'il était plutôt qu'un formulaire vide, sans passer par un `RouteReuseStrategy`
 * global. Les résultats viennent d'un `POST` (calcul à la volée, jamais persisté côté serveur) :
 * il n'y a pas de ressource à récupérer par id, donc pas de sens à en faire un vrai appel réseau
 * depuis l'écran de détail. Se vide donc à chaque rechargement de page.
 */
@Injectable({
  providedIn: 'root'
})
export class PaieResultsService {

  results: PaieCalculResponse[] = [];
  selectedEmployees: Employee[] = [];
  searchQuery = '';
  formValue: PaieFormValue | null = null;

  /**
   * `true` seulement entre le clic sur "Retour" (écran de détail) et l'arrivée sur l'écran de
   * calcul, qui la remet à `false` après lecture (`PaieCalculComponent.ngOnInit`). Ce n'est donc
   * que ce chemin précis qui restaure l'état — toute autre façon d'arriver sur l'écran de calcul
   * (accueil, barre de navigation, fil d'Ariane, URL directe) repart d'un formulaire vide.
   */
  restoreOnNextLoad = false;

  findByEmployeeId(employeeId: number): PaieCalculResponse | undefined {
    return this.results.find(result => result.employee.id === employeeId);
  }

  reset(): void {
    this.results = [];
    this.selectedEmployees = [];
    this.searchQuery = '';
    this.formValue = null;
  }
}
