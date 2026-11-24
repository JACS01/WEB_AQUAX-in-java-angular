import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { TranslationService } from '../../core/i18n/translation.service';

@Component({
  selector: 'app-admin-field-error',
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `@if (control().touched && control().invalid) {
    <small class="field-error" role="alert">{{ message() }}</small>
  }`,
  styles: [
    `
      .field-error {
        display: block;
        color: #b42318;
        font-size: 0.78rem;
        margin-top: 0.3rem;
      }
    `,
  ],
})
export class AdminFieldError {
  private readonly i18n = inject(TranslationService);
  control = input.required<AbstractControl>();
  message() {
    const errors = this.control().errors ?? {};
    if (errors['required']) return this.i18n.t('validation.required');
    if (errors['email']) return this.i18n.t('validation.adminEmail');
    if (errors['minlength'])
      return this.i18n.t('validation.minCharacters', { count: errors['minlength'].requiredLength });
    if (errors['maxlength'])
      return this.i18n.t('validation.maxCharacters', { count: errors['maxlength'].requiredLength });
    if (errors['passwordBytes']) return this.i18n.t('validation.passwordBytes');
    if (errors['integerId']) return this.i18n.t('validation.account');
    if (errors['isoDate']) return this.i18n.t('validation.adminDate');
    if (errors['oneOf']) return this.i18n.t('validation.adminOption');
    if (errors['pattern']) return this.i18n.t('validation.pattern');
    return this.i18n.t('validation.review');
  }
}
