import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const notBlank: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  typeof control.value !== 'string' || !control.value.trim() ? { required: true } : null;

export const passwordBytes: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  typeof control.value === 'string' && new TextEncoder().encode(control.value).length > 72
    ? { passwordBytes: true }
    : null;

export const integerId: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  typeof control.value === 'number' && Number.isSafeInteger(control.value) && control.value > 0
    ? null
    : { integerId: true };
