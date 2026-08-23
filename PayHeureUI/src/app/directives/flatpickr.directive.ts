import { Directive, ElementRef, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import flatpickr from 'flatpickr';
import { Instance } from 'flatpickr/dist/types/instance';
import { Options } from 'flatpickr/dist/types/options';

/**
 * Attache flatpickr — un calendrier/horloge dessinés par la librairie, pas les widgets natifs du
 * navigateur — à un `<input>` de formulaire réactif.
 *
 * Contrairement à `<input type="date"/"time">`, dont le format d'affichage et de saisie dépend de
 * la langue/région du système ou du navigateur (d'où les soucis vécus ici : MM/dd/yyyy au lieu de
 * dd-MM-yyyy, AM/PM au lieu de 24h), flatpickr dessine sa propre interface : le `dateFormat` qu'on
 * lui donne en `appFlatpickr` est ce que l'utilisateur voit, sur tous les navigateurs et systèmes.
 *
 * `appFlatpickr` peut changer après l'init (ex. `minDate`/`maxDate` recalculés quand un autre champ
 * de la page change) : `ngOnChanges` répercute alors la nouvelle config sur l'instance existante au
 * lieu de la recréer.
 */
@Directive({
  selector: '[appFlatpickr]',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => FlatpickrDirective),
    multi: true
  }]
})
export class FlatpickrDirective implements OnInit, OnChanges, OnDestroy, ControlValueAccessor {

  @Input() appFlatpickr: Partial<Options> = {};

  private instance?: Instance;
  private pendingValue: string | null = null;
  private onChangeFn: (value: string) => void = () => {};
  private onTouchedFn: () => void = () => {};

  /**
   * Dernière valeur connue du `FormControl` (remontée par flatpickr ou écrite via `writeValue`),
   * pour éviter à `onBlur` de la renvoyer inutilement — voir son commentaire.
   */
  private lastValue: string | null = null;

  constructor(private readonly elementRef: ElementRef<HTMLInputElement>) {}

  ngOnInit(): void {
    this.instance = flatpickr(this.elementRef.nativeElement, {
      allowInput: true,
      ...this.appFlatpickr,
      onChange: (_dates, dateStr) => {
        this.lastValue = dateStr;
        this.onChangeFn(dateStr);
      },
      onClose: () => this.onTouchedFn()
    });

    // Filet de sécurité pour la saisie manuelle (allowInput) : quand on vide le champ au clavier,
    // flatpickr.js déclenche `setDate("", ...)` → `clear()` sur blur, mais ne garantit pas dans
    // tous les cas d'enchaînement d'événements (ex. blur suivi immédiatement d'un clic sur le
    // bouton "Calculer") que son propre `onChange` remonte bien la valeur vide au FormControl —
    // constaté en pratique : l'input affichait vide mais l'ancienne valeur partait quand même au
    // backend. On resynchronise donc explicitement le FormControl sur ce que l'input affiche
    // réellement à chaque perte de focus, plutôt que de dépendre uniquement du hook de flatpickr.
    // Enregistré après flatpickr lui-même : son propre blur (qui reformate/vide l'input) s'exécute
    // avant, donc `nativeElement.value` est déjà la valeur finale quand on la lit ici.
    this.elementRef.nativeElement.addEventListener('blur', this.onBlur);

    // `writeValue` peut être appelé par Angular avant que l'instance flatpickr n'existe : on
    // rejoue la dernière valeur reçue une fois l'instance prête.
    if (this.pendingValue !== null) {
      this.instance.setDate(this.pendingValue, false);
    }
  }

  /**
   * Ne remonte la valeur au `FormControl` que si elle a réellement changé depuis la dernière fois
   * (`lastValue`) : sans cette garde, tout blur du champ — y compris cliquer dans une zone vide du
   * calendrier flatpickr encore ouvert, qui n'est pas rattrapé par la logique interne de la
   * librairie mais blur quand même l'input nativement — remontait la valeur inchangée au
   * `FormControl`. Or `FormControl.setValue` déclenche `valueChanges` même à valeur identique, ce
   * qui, sur l'écran de calcul de paie, effaçait à tort le message d'erreur "aucun salarié trouvé"
   * sans qu'aucune nouvelle date n'ait été choisie (voir PaieCalculComponent.ngOnInit).
   */
  private readonly onBlur = (): void => {
    const value = this.elementRef.nativeElement.value;
    if (value !== this.lastValue) {
      this.lastValue = value;
      this.onChangeFn(value);
    }
    this.onTouchedFn();
  };

  ngOnChanges(changes: SimpleChanges): void {
    const change = changes['appFlatpickr'];
    if (change && !change.firstChange && this.instance) {
      this.instance.set(this.appFlatpickr);
    }
  }

  ngOnDestroy(): void {
    this.elementRef.nativeElement.removeEventListener('blur', this.onBlur);
    this.instance?.destroy();
  }

  writeValue(value: string): void {
    // Tient `lastValue` à jour même ici (voir son commentaire dans `onBlur`) : une valeur écrite
    // par le formulaire (ex. `patchValue`, restauration au retour de l'écran de détail) ne doit
    // pas être reprise pour "changée" par le prochain blur simplement parce qu'elle n'était pas
    // passée par flatpickr lui-même.
    this.lastValue = value ?? '';
    if (this.instance) {
      this.instance.setDate(value ?? '', false);
    } else {
      this.pendingValue = value;
    }
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChangeFn = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouchedFn = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.instance?.set('clickOpens', !isDisabled);
    this.elementRef.nativeElement.disabled = isDisabled;
  }
}
