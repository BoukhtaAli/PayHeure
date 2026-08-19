/**
 * Formats de date/heure partagés par les écrans qui saisissent une période (calcul de paie,
 * recherche de salariés) : `dd-MM-yyyy` et `HH:mm` (24h) fixes, jamais délégués à un
 * `<input type="date"/"time">` natif dont l'affichage dépend de la langue/région du navigateur
 * (voir flatpickr.directive.ts).
 */

/** Format `dd-MM-yyyy` fixe, jamais `MM/dd/yyyy`. */
export const DATE_PATTERN = /^(0[1-9]|[12]\d|3[01])-(0[1-9]|1[0-2])-\d{4}$/;

/** Format `HH:mm` sur 24h (00-23), jamais AM/PM. */
export const HEURE_PATTERN = /^([01]\d|2[0-3]):[0-5]\d$/;

/** Convertit une date saisie `dd-MM-yyyy` en `yyyy-MM-dd` (ISO, attendu par le backend). */
export function toIsoDate(dateSaisie: string): string {
  const [jour, mois, annee] = dateSaisie.split('-');
  return `${annee}-${mois}-${jour}`;
}

/** Combine une date (`dd-MM-yyyy`) et une heure (`HH:mm`) en `LocalDateTime` ISO. */
export function toIsoDateTime(date: string, heure: string): string {
  return `${toIsoDate(date)}T${heure}`;
}

/** Inverse de `toIsoDateTime` côté affichage : `yyyy-MM-ddTHH:mm:ss` → `dd-MM-yyyy HH:mm`. */
export function fromIsoDateTime(iso: string): string {
  const [datePart, timePart] = iso.split('T');
  const [annee, mois, jour] = datePart.split('-');
  return `${jour}-${mois}-${annee} ${timePart.slice(0, 5)}`;
}
