import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { Preferencias } from '../core/models';
import { PreferencesService } from '../core/preferences.service';
import { TranslationService } from '../core/i18n/translation.service';
import en from '../core/i18n/en.json';
import pt from '../core/i18n/pt.json';
import { PanelLayout } from './panel.layout';

const spanish: Preferencias = {
  usuarioId: 1,
  idioma: 'es',
  nivelZoom: 100,
  filtroColor: 'none',
  filtroColorActivo: false,
};

describe('PanelLayout: account language and translated navigation', () => {
  let http: HttpTestingController;
  let preferences: PreferencesService;
  let translations: TranslationService;
  const auth = {
    user: signal({ nombre: 'Ana', apellido: 'Pérez' }),
    isAdmin: signal(true),
    logout: vi.fn(),
  };

  beforeEach(() => {
    auth.isAdmin.set(true);
    auth.logout.mockClear();
    TestBed.configureTestingModule({
      imports: [PanelLayout],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    preferences = TestBed.inject(PreferencesService);
    translations = TestBed.inject(TranslationService);
  });

  afterEach(() => {
    http.verify();
    preferences.reset();
  });

  function panel() {
    const fixture = TestBed.createComponent(PanelLayout);
    fixture.detectChanges();
    return fixture;
  }

  it('loads saved language on entry and retrieves it again in a newly created panel', () => {
    const first = panel();
    http.expectOne('/api/personal/preferencias').flush({ ...spanish, idioma: 'en' });
    first.detectChanges();
    expect(first.nativeElement.querySelector('a[href="/panel/pedidos"]').textContent).toContain(
      'Orders',
    );
    first.destroy();
    preferences.reset();
    expect(translations.language()).toBe('es');
    const restored = panel();
    http.expectOne('/api/personal/preferencias').flush({ ...spanish, idioma: 'en' });
    restored.detectChanges();
    expect(translations.language()).toBe('en');
    expect(restored.nativeElement.querySelector('a[href="/panel/pedidos"]').textContent).toContain(
      'Orders',
    );
  });

  it('updates navigation, accessible labels and date when a confirmed preference changes', () => {
    const fixture = panel();
    http.expectOne('/api/personal/preferencias').flush(spanish);
    for (const [language, locale, catalog] of [
      ['en', 'en-US', en],
      ['pt', 'pt-BR', pt],
    ] as const) {
      preferences.apply({ ...spanish, idioma: language });
      fixture.componentInstance.menuOpen.set(true);
      fixture.detectChanges();
      const dom: HTMLElement = fixture.nativeElement;
      expect(dom.getAttribute('lang')).toBe(language);
      expect(dom.querySelector('#main-content')?.getAttribute('lang')).toBe(language);
      expect(dom.querySelector('a[href="/panel/reportes"]')?.textContent).toContain(
        catalog['layout.reports'],
      );
      expect(dom.querySelector('a[href="/panel/usuarios"]')?.textContent).toContain(
        catalog['layout.users'],
      );
      expect(dom.querySelector('.account small')?.textContent).toContain(catalog['layout.admin']);
      expect(dom.querySelector('.account button')?.getAttribute('aria-label')).toBe(
        catalog['layout.logout'],
      );
      expect(dom.querySelector('.account button')?.getAttribute('title')).toBe(
        catalog['layout.logout'],
      );
      expect(dom.querySelector('.mobile-menu')?.getAttribute('aria-label')).toBe(
        catalog['layout.openMenu'],
      );
      expect(dom.querySelector('.nav-backdrop')?.getAttribute('aria-label')).toBe(
        catalog['layout.closeMenu'],
      );
      expect(dom.querySelector('.local-date')?.textContent).toBe(
        new Intl.DateTimeFormat(locale, {
          day: 'numeric',
          month: 'long',
          year: 'numeric',
          timeZone: 'America/Lima',
        }).format(new Date()),
      );
    }
    auth.isAdmin.set(false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/panel/usuarios"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('.account small').textContent).toContain(
      pt['layout.seller'],
    );
  });

  it('does not let a delayed initial read replace a more recent saved language', () => {
    const fixture = panel();
    const initialRead = http.expectOne('/api/personal/preferencias');
    preferences.apply({ ...spanish, idioma: 'pt' });
    initialRead.flush({ ...spanish, idioma: 'en' });
    fixture.detectChanges();
    expect(translations.language()).toBe('pt');
  });

  it('keeps the last saved language if the older initial read fails', () => {
    panel();
    const initialRead = http.expectOne('/api/personal/preferencias');
    preferences.apply({ ...spanish, idioma: 'pt' });
    initialRead.error(new ProgressEvent('network'));
    expect(translations.language()).toBe('pt');
  });

  it('uses Spanish if preferences cannot be loaded and no newer preference was saved', () => {
    translations.use('en');
    panel();
    http.expectOne('/api/personal/preferencias').error(new ProgressEvent('network'));
    expect(translations.language()).toBe('es');
  });

  it('clears language on logout and loads the next account language independently', () => {
    const first = panel();
    http.expectOne('/api/personal/preferencias').flush({ ...spanish, idioma: 'en' });
    first.componentInstance.logout();
    expect(auth.logout).toHaveBeenCalledOnce();
    expect(translations.language()).toBe('es');
    first.destroy();
    panel();
    http.expectOne('/api/personal/preferencias').flush({ ...spanish, usuarioId: 2, idioma: 'pt' });
    expect(translations.language()).toBe('pt');
  });
});
