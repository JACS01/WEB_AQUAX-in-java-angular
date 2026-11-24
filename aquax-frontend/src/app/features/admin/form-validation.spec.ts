import { FormControl } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import { integerId, notBlank, passwordBytes } from './form-validation';

describe('Validaciones administrativas', () => {
  it('rechaza un nombre compuesto solo de espacios', () => {
    expect(notBlank(new FormControl('   '))).toEqual({ required: true });
    expect(notBlank(new FormControl(' Ana '))).toBeNull();
    expect(notBlank(new FormControl(null))).toEqual({ required: true });
    expect(notBlank(new FormControl(123))).toEqual({ required: true });
  });
  it('aplica el límite BCrypt en bytes para contraseñas Unicode', () => {
    expect(passwordBytes(new FormControl('á'.repeat(36)))).toBeNull();
    expect(passwordBytes(new FormControl('á'.repeat(37)))).toEqual({ passwordBytes: true });
    expect(passwordBytes(new FormControl('a'.repeat(72)))).toBeNull();
  });
  it('rechaza identificadores fraccionarios, cero y desbordados', () => {
    for (const value of [0, -1, 1.5, Number.MAX_SAFE_INTEGER + 1]) {
      expect(integerId(new FormControl(value))).toEqual({ integerId: true });
    }
    expect(integerId(new FormControl(25))).toBeNull();
  });
});
