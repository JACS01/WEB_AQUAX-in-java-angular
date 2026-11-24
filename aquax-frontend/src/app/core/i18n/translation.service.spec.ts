import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { PreferencesService } from '../preferences.service';
import es from './es.json';
import en from './en.json';
import pt from './pt.json';
import { TranslationService } from './translation.service';

describe('TranslationService', () => {
  let translations: TranslationService;
  let doc: Document;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    translations = TestBed.inject(TranslationService);
    doc = TestBed.inject(DOCUMENT);
  });

  afterEach(() => TestBed.inject(PreferencesService).reset());

  it('keeps matching nonempty catalogs for all supported languages', () => {
    const keys = Object.keys(es).sort();
    expect(Object.keys(en).sort()).toEqual(keys);
    expect(Object.keys(pt).sort()).toEqual(keys);
    for (const catalog of [es, en, pt]) {
      for (const value of Object.values(catalog)) {
        expect(typeof value).toBe('string');
        expect(value.trim().length).toBeGreaterThan(0);
      }
    }
  });

  it('switches texts and locale while leaving the document base language unchanged', () => {
    doc.documentElement.lang = 'es';
    expect(translations.language()).toBe('es');
    expect(translations.t('layout.orders')).toBe('Pedidos');
    translations.use('en');
    expect(translations.t('layout.orders')).toBe('Orders');
    expect(translations.locale()).toBe('en-US');
    expect(doc.documentElement.lang).toBe('es');
    translations.use('pt');
    expect(translations.t('layout.orders')).toBe('Pedidos');
    expect(translations.locale()).toBe('pt-BR');
    expect(doc.documentElement.lang).toBe('es');
    translations.use('es');
    expect(translations.locale()).toBe('es-PE');
    expect(doc.documentElement.lang).toBe('es');
  });

  it.each(['fr', '', 'constructor', '__proto__', 'EN'])(
    'falls back to Spanish for unsupported account language %s',
    (language) => {
      doc.documentElement.lang = 'es';
      translations.use('pt');
      translations.use(language);
      expect(translations.language()).toBe('es');
      expect(translations.locale()).toBe('es-PE');
      expect(translations.t('layout.orders')).toBe('Pedidos');
      expect(doc.documentElement.lang).toBe('es');
    },
  );

  it('applies account language with visual preferences and clears both on reset', () => {
    const preferences = TestBed.inject(PreferencesService);
    preferences.apply({
      usuarioId: 7,
      idioma: 'en',
      nivelZoom: 120,
      filtroColor: 'grayscale',
      filtroColorActivo: true,
    });
    expect(translations.language()).toBe('en');
    expect(doc.documentElement.style.fontSize).toBe('19.2px');
    expect(doc.documentElement.style.filter).toBe('grayscale(1)');
    preferences.reset();
    expect(translations.language()).toBe('es');
    expect(doc.documentElement.style.fontSize).toBe('');
    expect(doc.documentElement.style.filter).toBe('');
  });
});
