import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export interface AppLanguage {
  code: 'fr' | 'en' | 'ar';
  label: string;
  /** Classe CSS du drapeau (package flag-icons), ex. "fi-fr". */
  flag: string;
  /** Sens d'écriture de la langue, appliqué à `<html dir>`. */
  dir: 'ltr' | 'rtl';
}

const STORAGE_KEY = 'app-lang';

/** Langue par défaut si aucune préférence n'est enregistrée ou reconnue. */
const DEFAULT_LANG: AppLanguage['code'] = 'fr';

/**
 * Gère la langue courante de l'application (traductions ngx-translate, persistance, sens
 * d'écriture). En ajouter une revient à déposer un `assets/i18n/<code>.json` et une entrée
 * dans `languages`.
 */
@Injectable({
  providedIn: 'root'
})
export class LanguageService {

  readonly languages: AppLanguage[] = [
    { code: 'fr', label: 'Français', flag: 'fi-fr', dir: 'ltr' },
    { code: 'en', label: 'English', flag: 'fi-us', dir: 'ltr' },
    { code: 'ar', label: 'العربية', flag: 'fi-ma', dir: 'rtl' }
  ];

  constructor(private readonly translate: TranslateService) {
    this.translate.addLangs(this.languages.map(lang => lang.code));
    this.translate.setDefaultLang(DEFAULT_LANG);
  }

  get currentLang(): AppLanguage {
    const code = this.translate.currentLang as AppLanguage['code'];
    return this.languages.find(lang => lang.code === code) ?? this.languages[0];
  }

  /** À appeler au démarrage de l'application : applique la langue enregistrée (ou par défaut). */
  init(): void {
    const saved = localStorage.getItem(STORAGE_KEY) as AppLanguage['code'] | null;
    const initial = this.languages.find(lang => lang.code === saved)?.code ?? DEFAULT_LANG;
    this.use(initial);
  }

  use(code: AppLanguage['code']): void {
    const language = this.languages.find(lang => lang.code === code) ?? this.languages[0];
    this.translate.use(language.code);
    localStorage.setItem(STORAGE_KEY, language.code);
    document.documentElement.lang = language.code;
    document.documentElement.dir = language.dir;
  }
}
