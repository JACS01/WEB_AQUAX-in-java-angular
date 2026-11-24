import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const notBlank: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  typeof control.value !== 'string' || !control.value.trim() ? { required: true } : null;

export const phoneFormat: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value: unknown = control.value;
  return typeof value === 'string' &&
    value.length > 0 &&
    !/^[+]?(?=[0-9() -]*[0-9])[0-9() -]+$/.test(value)
    ? { phoneFormat: true }
    : null;
};

export function maxUtf8Bytes(maximum: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null =>
    typeof control.value === 'string' && new TextEncoder().encode(control.value).length > maximum
      ? { maxUtf8Bytes: { maximum } }
      : null;
}

/** Empty dates can be combined with Validators.required when the DTO requires them. */
export const isoDate: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value: unknown = control.value;
  if (value === '' || value === null || value === undefined) return null;
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return { isoDate: true };
  const [year, month, day] = value.split('-').map(Number);
  const date = new Date(0);
  date.setUTCFullYear(year, month - 1, day);
  date.setUTCHours(0, 0, 0, 0);
  return date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day
    ? null
    : { isoDate: true };
};

export function oneOf(values: readonly unknown[]): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null =>
    values.includes(control.value) ? null : { oneOf: true };
}

export function safeInteger(minimum: number, maximum: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null =>
    typeof control.value === 'number' &&
    Number.isSafeInteger(control.value) &&
    control.value >= minimum &&
    control.value <= maximum
      ? null
      : { integer: true };
}
