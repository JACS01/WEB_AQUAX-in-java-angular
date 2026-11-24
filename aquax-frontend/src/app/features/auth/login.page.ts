import { TranslationService } from '../../core/i18n/translation.service';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { Icon } from '../../shared/icon';
import { FieldError } from '../../shared/field-error';
import { notBlank } from '../../shared/validators';
import { LanguageSwitcher } from '../../shared/language-switcher';

@Component({
  host: { '[attr.lang]': 'i18n.language()' },
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, Icon, FieldError, LanguageSwitcher],
  templateUrl: './login.page.html',
  styles: ['.login-box > app-language-switcher { display: block; margin-bottom: 20px; }'],
})
export class LoginPage {
  readonly i18n = inject(TranslationService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly sessionExpired = this.route.snapshot.queryParamMap.has('expired');
  readonly busy = signal(false);
  private readonly requestError = signal<{ value: unknown } | null>(null);
  readonly error = computed(() => {
    const error = this.requestError();
    return error ? this.i18n.error(error.value) : '';
  });
  readonly form = inject(FormBuilder).nonNullable.group({
    nombreUsuario: ['', [notBlank, Validators.maxLength(50)]],
    contrasena: ['', [notBlank, Validators.maxLength(72)]],
  });
  submit() {
    if (this.busy()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.requestError.set(null);
    this.auth
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => {
          const target = this.route.snapshot.queryParamMap.get('returnUrl');
          void this.router.navigateByUrl(target?.startsWith('/panel') ? target : '/panel');
        },
        error: (e: unknown) => this.requestError.set({ value: e }),
      });
  }
}
