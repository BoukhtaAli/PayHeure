import { Component, ElementRef, AfterViewInit, OnDestroy, OnInit } from '@angular/core';
import { AppLanguage, LanguageService } from '../../services/language.service';

@Component({
  selector: 'nav-bar',
  templateUrl: './nav-bar.component.html',
  styleUrls: ['./nav-bar.component.css']
})
export class NavBarComponent implements OnInit, AfterViewInit, OnDestroy {

  private resizeObserver!: ResizeObserver;

  mainMenu: MenuItem[] = [];

  constructor(
    private elementRef: ElementRef,
    private readonly languageService: LanguageService
  ) {}

  ngOnInit(): void {
    this.mainMenu.push(
      { labelKey: 'NAV.HOME', icon: 'fas fa-house', route: 'home' },
      { labelKey: 'NAV.PAIE', icon: 'fas fa-calculator', route: 'paie' });
  }

  get languages(): AppLanguage[] {
    return this.languageService.languages;
  }

  get currentLang(): AppLanguage {
    return this.languageService.currentLang;
  }

  switchLang(code: AppLanguage['code']): void {
    this.languageService.use(code);
  }

  ngAfterViewInit(): void {
    const navEl = this.elementRef.nativeElement.querySelector('.navbar');
    if (!navEl) return;

    this.resizeObserver = new ResizeObserver(() => {
      // `getBoundingClientRect` inclut le padding, contrairement à `entry.contentRect` : c'est la
      // hauteur visuelle réelle de la navbar qu'il faut réserver au contenu qui suit.
      const height = navEl.getBoundingClientRect().height;
      document.documentElement.style.setProperty('--navbar-height-actual', `${height}px`);
    });
    this.resizeObserver.observe(navEl);
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
  }
}

class MenuItem {
  labelKey!: string;
  icon!: string;
  route!: string;
}
