import { TestBed } from '@angular/core/testing';
import { TranslationService } from '../core/i18n/translation.service';
import { LanguageSwitcher } from './language-switcher';

describe('Selector de idioma público', () => {
  it('permite elegir idioma localmente sin solicitar ni guardar preferencias en el backend', () => {
    const fixture = TestBed.createComponent(LanguageSwitcher);
    fixture.detectChanges();
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('select');
    select.value = 'en';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(TestBed.inject(TranslationService).language()).toBe('en');
    expect(fixture.nativeElement.textContent).toContain('Language');
    expect(fixture.nativeElement.getAttribute('lang')).toBe('en');
    select.value = 'pt';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(TestBed.inject(TranslationService).language()).toBe('pt');
    expect(fixture.nativeElement.getAttribute('lang')).toBe('pt');
  });
});
