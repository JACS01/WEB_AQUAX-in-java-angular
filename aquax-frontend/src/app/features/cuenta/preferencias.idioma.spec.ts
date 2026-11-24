import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TranslationService } from '../../core/i18n/translation.service';
import { Preferencias } from '../../core/models';
import { PreferencesService } from '../../core/preferences.service';
import { PreferenciasPage } from './preferencias.page';

const stored: Preferencias = {
  usuarioId: 1,
  idioma: 'es',
  nivelZoom: 100,
  filtroColor: 'none',
  filtroColorActivo: false,
};

describe('Idioma de preferencias: confirmación y pantalla', () => {
  let http: HttpTestingController;
  let i18n: TranslationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PreferenciasPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    i18n = TestBed.inject(TranslationService);
  });

  afterEach(() => {
    http.verify();
    TestBed.inject(PreferencesService).reset();
  });

  function open() {
    const fixture = TestBed.createComponent(PreferenciasPage);
    fixture.detectChanges();
    http.expectOne('/api/personal/preferencias').flush(stored);
    fixture.detectChanges();
    return fixture;
  }

  it('solo aplica el idioma confirmado y traduce el aviso de éxito en esa misma pantalla', () => {
    const fixture = open();
    const page = fixture.componentInstance;
    const select: HTMLSelectElement = fixture.nativeElement.querySelector(
      '[formControlName="idioma"]',
    );
    select.value = 'en';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(page.form.controls.idioma.value).toBe('en');
    expect(i18n.language()).toBe('es');
    page.save();
    const request = http.expectOne('/api/personal/preferencias');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body.idioma).toBe('en');
    expect(i18n.language()).toBe('es');
    request.flush({ ...stored, idioma: 'en' });
    fixture.detectChanges();
    expect(i18n.language()).toBe('en');
    expect(fixture.nativeElement.getAttribute('lang')).toBe('en');
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('Preferences');
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain('saved');
    expect(fixture.nativeElement.querySelector('button.button').textContent).toContain('Save');
    expect(fixture.nativeElement.querySelector('[formControlName="idioma"]').value).toBe('en');
  });

  it('recupera el idioma de la cuenta al cargar o reintentar la pantalla de preferencias', () => {
    const fixture = TestBed.createComponent(PreferenciasPage);
    fixture.detectChanges();
    http.expectOne('/api/personal/preferencias').error(new ProgressEvent('network'));
    expect(i18n.language()).toBe('es');
    fixture.componentInstance.load();
    http.expectOne('/api/personal/preferencias').flush({ ...stored, idioma: 'en' });
    fixture.detectChanges();
    expect(i18n.language()).toBe('en');
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('Preferences');
    expect(fixture.componentInstance.form.controls.idioma.value).toBe('en');
  });

  it('no cambia idioma si falla el guardado y permite reintentarlo conservando la selección', () => {
    const fixture = open();
    const page = fixture.componentInstance;
    page.form.controls.idioma.setValue('pt');
    page.save();
    http
      .expectOne('/api/personal/preferencias')
      .flush({ detail: 'Error interno' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(i18n.language()).toBe('es');
    expect(page.form.controls.idioma.value).toBe('pt');
    expect(page.saved()).toBe(false);
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
    page.save();
    http.expectOne('/api/personal/preferencias').flush({ ...stored, idioma: 'pt' });
    fixture.detectChanges();
    expect(i18n.language()).toBe('pt');
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('Preferências');
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain('salvas');
  });

  it('restablecer solo modifica el formulario hasta guardar, sin escrituras ni cambios de idioma', () => {
    const fixture = open();
    const page = fixture.componentInstance;
    page.form.controls.idioma.setValue('en');
    page.save();
    http.expectOne('/api/personal/preferencias').flush({ ...stored, idioma: 'en' });
    page.reset();
    fixture.detectChanges();
    expect(page.form.controls.idioma.value).toBe('es');
    expect(i18n.language()).toBe('en');
    expect(page.saved()).toBe(false);
    http.expectNone((request) => request.method === 'PUT');
  });
});
