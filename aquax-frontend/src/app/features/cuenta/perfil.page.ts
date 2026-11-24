import { TranslationService } from '../../core/i18n/translation.service';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { CuentaService } from './cuenta.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth/auth.service';
import { FieldError } from '../../shared/field-error';
import { maxUtf8Bytes, notBlank } from '../../shared/validators';
@Component({
  host: { '[attr.lang]': 'i18n.language()' },
  selector: 'app-perfil',
  imports: [ReactiveFormsModule, FieldError],
  templateUrl: './perfil.page.html',
})
export class PerfilPage implements OnInit {
  readonly i18n = inject(TranslationService);
  readonly auth = inject(AuthService);
  private readonly service = inject(CuentaService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder).nonNullable;
  readonly loading = signal(false);
  readonly loaded = signal(false);
  readonly loadError = signal('');
  readonly error = signal('');
  readonly saved = signal(false);
  readonly busy = signal(false);
  readonly passwordBusy = signal(false);
  readonly passwordError = signal('');
  readonly form = this.fb.group({
    nombre: ['', [notBlank, Validators.maxLength(100)]],
    apellido: ['', [notBlank, Validators.maxLength(100)]],
    correo: ['', [notBlank, Validators.email, Validators.maxLength(150)]],
    telefono: ['', [notBlank, Validators.maxLength(15)]],
  });
  readonly passwordForm = this.fb.group({
    contrasenaActual: ['', [notBlank, Validators.maxLength(72), maxUtf8Bytes(72)]],
    nuevaContrasena: [
      '',
      [notBlank, Validators.minLength(8), Validators.maxLength(72), maxUtf8Bytes(72)],
    ],
    confirmarContrasena: ['', [notBlank, Validators.maxLength(72)]],
  });
  ngOnInit() {
    this.load();
  }
  load() {
    if (this.loading() || this.busy()) return;
    this.loading.set(true);
    this.loaded.set(false);
    this.loadError.set('');
    this.service
      .perfil()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (p) => {
          this.form.patchValue({ ...p, telefono: p.telefono ?? '' });
          this.loaded.set(true);
        },
        error: (e) => this.loadError.set(this.i18n.error(e)),
      });
  }
  save() {
    if (this.busy() || this.loading() || !this.loaded()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.saved.set(false);
    this.service
      .actualizarPerfil(this.form.getRawValue())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: (p) => {
          this.auth.updateUser(p);
          this.saved.set(true);
        },
        error: (e) => this.error.set(this.i18n.error(e)),
      });
  }
  changePassword() {
    if (this.passwordBusy()) return;
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    const p = this.passwordForm.getRawValue();
    this.passwordError.set('');
    if (p.nuevaContrasena !== p.confirmarContrasena) {
      this.passwordError.set(this.i18n.t('profile.passwordMismatch'));
      return;
    }
    if (new TextEncoder().encode(p.nuevaContrasena).length > 72) {
      this.passwordError.set(this.i18n.t('profile.passwordBytes'));
      return;
    }
    this.passwordBusy.set(true);
    this.service
      .cambiarContrasena(p)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.passwordBusy.set(false)),
      )
      .subscribe({
        next: () => this.auth.logout(),
        error: (e) => this.passwordError.set(this.i18n.error(e)),
      });
  }
}
