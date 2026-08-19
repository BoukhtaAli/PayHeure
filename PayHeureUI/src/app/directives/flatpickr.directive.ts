import { Directive, ElementRef, Input, OnDestroy, OnInit, forwardRef } from '@angular/core';
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
 */
@Directive({
  selector: '[appFlatpickr]',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => FlatpickrDirective),
    multi: true
  }]
})
export class FlatpickrDirective implements OnInit, OnDestroy, ControlValueAccessor {

  @Input() appFlatpickr: Partial<Options> = {};

  private instance?: Instance;
  private pendingValue: string | null = null;
  private onChangeFn: (value: string) => void = () => {};
  private onTouchedFn: () => void = () => {};

  constructor(private readonly elementRef: ElementRef<HTMLInputElement>) {}

  ngOnInit(): void {
    this.instance = flatpickr(this.elementRef.nativeElement, {
      allowInput: true,
      ...this.appFlatpickr,
      onChange: (_dates, dateStr) => this.onChangeFn(dateStr),
      onClose: () => this.onTouchedFn()
    });

    // `writeValue` peut être appelé par Angular avant que l'instance flatpickr n'existe : on
    // rejoue la dernière valeur reçue une fois l'instance prête.
    if (this.pendingValue !== null) {
      this.instance.setDate(this.pendingValue, false);
    }
  }

  ngOnDestroy(): void {
    this.instance?.destroy();
  }

  writeValue(value: string): void {
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
