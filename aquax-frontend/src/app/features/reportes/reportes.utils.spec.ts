import { describe, expect, it } from 'vitest';
import { nombreCsv, rangoValido } from './reportes.utils';

describe('Filtros del reporte', () => {
  it.each([
    '2025-02-29',
    '1900-02-29',
    '2026-04-31',
    '2026-13-01',
    '2026-00-01',
    '2026-01-00',
    '2026-1-01',
    ' 2026-01-01',
    '2026-01-01T00:00:00',
  ])('rechaza la fecha inexistente o no ISO %s', (fecha) => {
    expect(rangoValido(fecha, '')).toBe(false);
    expect(rangoValido('', fecha)).toBe(false);
  });
  it('admite rangos inclusivos y extremos opcionales', () => {
    expect(rangoValido('', '')).toBe(true);
    expect(rangoValido('2026-11-11', '')).toBe(true);
    expect(rangoValido('', '2026-11-11')).toBe(true);
    expect(rangoValido('2026-11-11', '2026-11-11')).toBe(true);
  });
  it('rechaza rangos invertidos, fechas inexistentes y formatos no ISO', () => {
    expect(rangoValido('2026-11-12', '2026-11-11')).toBe(false);
    expect(rangoValido('2026-02-30', '')).toBe(false);
    expect(rangoValido('11/11/2026', '')).toBe(false);
    expect(rangoValido('2024-02-29', '2024-03-01')).toBe(true);
  });
});

describe('Nombre seguro del CSV', () => {
  it('usa el nombre del servidor y admite la codificación UTF-8', () => {
    expect(nombreCsv('attachment; filename="aquax-historial-2026-11-11.csv"')).toBe(
      'aquax-historial-2026-11-11.csv',
    );
    expect(nombreCsv("attachment; filename*=UTF-8''reporte%20Aquax.csv")).toBe('reporte Aquax.csv');
  });
  it('elimina rutas y evita extensiones ajenas y codificación inválida', () => {
    expect(nombreCsv('attachment; filename="../../reporte.csv"')).toBe('reporte.csv');
    expect(nombreCsv('attachment; filename="archivo.exe"')).toBe('aquax-historial.csv');
    expect(nombreCsv("attachment; filename*=UTF-8''%ZZ.csv")).toBe('aquax-historial.csv');
    expect(nombreCsv(null)).toBe('aquax-historial.csv');
  });
});
