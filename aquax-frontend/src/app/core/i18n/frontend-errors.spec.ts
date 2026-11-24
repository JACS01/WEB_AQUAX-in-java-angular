import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { TranslationService } from './translation.service';

describe('Mensajes HTTP presentados en Angular', () => {
  let i18n: TranslationService;
  beforeEach(() => {
    i18n = TestBed.inject(TranslationService);
  });

  it.each(['en', 'pt'])(
    'muestra equivalentes locales en %s sin modificar la respuesta del servidor',
    (language) => {
      i18n.use(language);
      const payload = { detail: 'El DNI ya está registrado por otro empleado.' };
      const error = new HttpErrorResponse({ status: 409, error: payload });
      expect(i18n.error(error)).toBe(i18n.t('serverErrors.duplicateDni'));
      expect(error.error).toBe(payload);
      expect(payload.detail).toBe('El DNI ya está registrado por otro empleado.');
      const unknown = new HttpErrorResponse({
        status: 500,
        error: { detail: 'Detalle interno desconocido.' },
      });
      expect(i18n.error(unknown)).toBe(i18n.t('errors.generic'));
      expect(i18n.error(unknown)).not.toContain('Detalle');
    },
  );

  it('indica los campos inválidos en el idioma elegido sin reutilizar mensajes españoles', () => {
    i18n.use('en');
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        detail: 'Revisa los campos indicados en errores.',
        errores: [
          { campo: 'correo', mensaje: 'Debe ser un correo válido.' },
          { campo: 'detalles[0].cantidad', mensaje: 'Debe ser mayor que cero.' },
        ],
      },
    });
    const text = i18n.error(error);
    expect(text).toContain('Email');
    expect(text).toContain('Quantity');
    expect(text).not.toContain('Debe');
    i18n.use('es');
    expect(i18n.error(error)).toContain('Debe ser un correo válido.');
  });

  it('interpola valores como datos y conserva las etiquetas del contrato sin traducir los datos libres', () => {
    i18n.use('en');
    expect(i18n.t('validation.minCharacters', { count: 8 })).toBe('Use at least 8 characters.');
    expect(i18n.t('errors.fields', { fields: '$& <cliente> {count}' })).toBe(
      'Check: $& <cliente> {count}.',
    );
    expect(i18n.label('EN_CAMINO')).toBe('On the way');
    expect(i18n.label('Ana Pérez')).toBe('Ana Pérez');
    expect(i18n.error(new HttpErrorResponse({ status: 0 }))).toBe(i18n.t('errors.network'));
  });
});
