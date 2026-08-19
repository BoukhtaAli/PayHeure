import { Component, Input } from '@angular/core';

/**
 * Un maillon du fil d'Ariane. `link` absent = élément courant (non cliquable, dernier de la liste).
 * `label` prime sur `labelKey` : utile pour un libellé dynamique déjà résolu (ex. nom du salarié),
 * qui n'a pas de clé de traduction.
 */
export interface BreadcrumbItem {
  labelKey?: string;
  label?: string;
  link?: string[];
}

@Component({
  selector: 'app-breadcrumb',
  templateUrl: './breadcrumb.component.html',
  styleUrls: ['./breadcrumb.component.css']
})
export class BreadcrumbComponent {
  @Input({ required: true }) items: BreadcrumbItem[] = [];
}
