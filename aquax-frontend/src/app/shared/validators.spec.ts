import { FormControl } from '@angular/forms';
import { isoDate, maxUtf8Bytes, notBlank, oneOf, safeInteger } from './validators';

describe('Validadores compartidos: límites de contrato', () => {
  it.each(['', ' ', '\t\n', null, undefined, false, 0])(
    'rechaza texto vacío o en blanco: %j',
    (value) => {
      expect(notBlank(new FormControl(value))).toEqual({ required: true });
    },
  );
  it.each(['2024-02-29', '2026-11-11', '0099-01-01', ''])(
    'acepta fecha de calendario válida u opcional: %s',
    (value) => {
      expect(isoDate(new FormControl(value))).toBeNull();
    },
  );
  it.each([
    '2026-02-29',
    '2026-02-30',
    '2026-13-01',
    '2026-00-01',
    '2026-01-00',
    '2026-04-31',
    '2026-1-1',
    '2026-11-11T12:00:00',
    'texto',
  ])('rechaza fecha inválida: %s', (value) => {
    expect(isoDate(new FormControl(value))).toEqual({ isoDate: true });
  });
  it('mide bytes UTF-8, incluidos caracteres multibyte, en el límite de BCrypt', () => {
    const validator = maxUtf8Bytes(72);
    expect(validator(new FormControl('a'.repeat(72)))).toBeNull();
    expect(validator(new FormControl('ñ'.repeat(36)))).toBeNull();
    expect(validator(new FormControl('ñ'.repeat(37)))).not.toBeNull();
    expect(validator(new FormControl('💧'.repeat(18)))).toBeNull();
    expect(validator(new FormControl('💧'.repeat(19)))).not.toBeNull();
  });
  it('valida enumeraciones y tipos sin convertir silenciosamente strings o números', () => {
    expect(oneOf([true, false])(new FormControl('false'))).not.toBeNull();
    expect(oneOf(['es', 'en', 'pt'])(new FormControl('fr'))).not.toBeNull();
    expect(oneOf(['es', 'en', 'pt'])(new FormControl('es'))).toBeNull();
    for (const value of [0, -1, 1.5, '1', NaN, Infinity, Number.MAX_SAFE_INTEGER + 1]) {
      expect(safeInteger(1, 50)(new FormControl(value))).not.toBeNull();
    }
    expect(safeInteger(1, 50)(new FormControl(50))).toBeNull();
  });
});
