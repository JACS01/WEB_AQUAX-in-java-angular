import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter, Router, TitleStrategy } from '@angular/router';
import { LocalizedTitleStrategy } from './localized-title.strategy';
import { TranslationService } from './translation.service';

@Component({ template: '' })
class Page {}

describe('Títulos de navegación traducidos', () => {
  it('cambia el título al guardar idioma y lo mantiene al navegar', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'preferencias', component: Page, title: 'Preferencias · AquaX' },
          { path: 'pedidos', component: Page, title: 'Pedidos · AquaX' },
        ]),
        { provide: TitleStrategy, useClass: LocalizedTitleStrategy },
      ],
    });
    const router = TestBed.inject(Router);
    const title = TestBed.inject(Title);
    const i18n = TestBed.inject(TranslationService);
    await router.navigateByUrl('/preferencias');
    expect(title.getTitle()).toBe('Preferencias · AquaX');
    i18n.use('en');
    TestBed.tick();
    expect(title.getTitle()).toBe('Preferences · AquaX');
    await router.navigateByUrl('/pedidos');
    expect(title.getTitle()).toBe('Orders · AquaX');
    i18n.use('pt');
    TestBed.tick();
    expect(title.getTitle()).toBe('Pedidos · AquaX');
  });
});
