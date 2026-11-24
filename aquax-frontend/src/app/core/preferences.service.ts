import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';
import { Preferencias } from './models';
import { TranslationService } from './i18n/translation.service';
@Injectable({ providedIn: 'root' })
export class PreferencesService {
  private readonly doc = inject(DOCUMENT);
  private readonly translations = inject(TranslationService);
  private generation = 0;

  get revision(): number {
    return this.generation;
  }

  apply(prefs: Preferencias) {
    this.generation++;
    this.translations.use(prefs.idioma);
    this.doc.documentElement.style.fontSize = `${(16 * prefs.nivelZoom) / 100}px`;
    const filters: Record<string, string> = {
      none: '',
      grayscale: 'grayscale(1)',
      contrast: 'contrast(1.4)',
      warm: 'sepia(.2)',
      cool: 'hue-rotate(15deg)',
      sepia: 'sepia(1)',
      invert: 'invert(.92) hue-rotate(180deg)',
      deuteranopia: 'url(#deuteranopia)',
      protanopia: 'url(#protanopia)',
      tritanopia: 'url(#tritanopia)',
    };
    this.doc.documentElement.style.filter = prefs.filtroColorActivo
      ? (filters[prefs.filtroColor] ?? '')
      : '';
  }
  reset() {
    this.generation++;
    this.translations.use('es');
    this.doc.documentElement.style.fontSize = '';
    this.doc.documentElement.style.filter = '';
  }
}
