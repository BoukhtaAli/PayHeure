/**
 * Export CSV partagé entre les écrans qui téléchargent leurs résultats (calcul de paie,
 * anomalies de pointage) : mêmes conventions RFC 4180 partout, un seul endroit à corriger si
 * elles changent.
 */

/** Échappe un champ CSV : entre guillemets, guillemets internes doublés (RFC 4180). */
function champCsv(valeur: string): string {
  return `"${valeur.replace(/"/g, '""')}"`;
}

/**
 * `yyyy-MM-dd-HH-mm-ss` en heure locale du navigateur — pas `Date.toISOString()`, qui est
 * toujours en UTC et afficherait donc une heure différente de celle de l'utilisateur.
 */
export function horodatageLocal(date: Date): string {
  const deuxChiffres = (n: number) => n.toString().padStart(2, '0');
  return [date.getFullYear(), deuxChiffres(date.getMonth() + 1), deuxChiffres(date.getDate()),
    deuxChiffres(date.getHours()), deuxChiffres(date.getMinutes()), deuxChiffres(date.getSeconds())]
    .join('-');
}

/**
 * Construit un CSV (séparateur `;`, BOM UTF-8) à partir d'en-têtes et de lignes déjà formatées,
 * puis déclenche son téléchargement sous `nomFichier`.
 */
export function telechargerCsv(nomFichier: string, entetes: string[], lignes: string[][]): void {
  // Séparateur `;` plutôt que `,` : convention des CSV en locale française (Excel FR utilise
  // la virgule comme séparateur décimal, donc `,` comme délimiteur de colonnes casse l'import).
  const csv = [entetes, ...lignes]
    .map(ligne => ligne.map(champCsv).join(';'))
    .join('\r\n');

  // BOM UTF-8 en préfixe (U+FEFF, code explicite pour éviter un caractère invisible dans la
  // source) : sans lui, Excel interprète les accents (salarié, prénom...) de travers.
  const bom = String.fromCharCode(0xFEFF);
  const blob = new Blob([bom + csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const lien = document.createElement('a');
  lien.href = url;
  lien.download = nomFichier;
  lien.click();
  URL.revokeObjectURL(url);
}
