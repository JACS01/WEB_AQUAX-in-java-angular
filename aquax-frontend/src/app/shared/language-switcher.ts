import { Component, inject } from '@angular/core';
import { TranslationService } from '../core/i18n/translation.service';

@Component({
  selector: 'app-language-switcher',
  host: { '[attr.lang]': 'i18n.language()' },
  template: `
    <label class="language-choice">
      <span class="sr-only">{{ i18n.t('common.language') }}</span>
      <select [value]="i18n.language()" (change)="change($event)">
        <option value="es">Español</option>
        <option value="en">English</option>
        <option value="pt">Português</option>
      </select>
    </label>
  `,
  styles: [
    `
      .language-choice select {
        font: inherit;
        color: inherit;
        background: transparent;
        border: 1px solid var(--border, #dce5e8);
        border-radius: 6px;
        padding: 6px 10px;
        max-width: 100%;
      }
      .language-choice option {
        color: var(--ink, #0a1f2e);
        background: #fff;
      }
    `,
  ],
})
export class LanguageSwitcher {
  readonly i18n = inject(TranslationService);
  change(event: Event): void {
    this.i18n.use((event.target as HTMLSelectElement).value);
  }
}
