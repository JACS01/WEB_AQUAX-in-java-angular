import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { TranslationService } from '../core/i18n/translation.service';
@Component({
  selector: 'app-field-error',
  changeDetection: ChangeDetectionStrategy.Eager,
  template:
    '@if (control()?.touched && control()?.invalid) { <small class="field-error" role="alert">{{ message() }}</small> }',
})
export class FieldError {
  private readonly i18n = inject(TranslationService);
  control = input<AbstractControl | null>(null);
  message() {
    const c = this.control();
    if (c?.hasError('required')) return this.i18n.t('validation.required');
    if (c?.hasError('email')) return this.i18n.t('validation.email');
    if (c?.hasError('minlength')) return this.i18n.t('validation.short');
    if (c?.hasError('maxlength')) return this.i18n.t('validation.long');
    if (c?.hasError('maxUtf8Bytes')) return this.i18n.t('validation.utf8');
    if (c?.hasError('isoDate')) return this.i18n.t('validation.date');
    if (c?.hasError('oneOf')) return this.i18n.t('validation.option');
    if (c?.hasError('integer')) return this.i18n.t('validation.integer');
    if (c?.hasError('min') || c?.hasError('max')) return this.i18n.t('validation.range');
    return this.i18n.t('validation.format');
  }
}
