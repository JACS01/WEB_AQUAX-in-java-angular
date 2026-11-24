import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { TranslationService } from '../../core/i18n/translation.service';
import { AdminService } from './admin.service';
import { AdminFieldError } from './field-error';
import { notBlank } from './form-validation';

@Component({
  selector: 'app-empresa-page',
  imports: [ReactiveFormsModule, AdminFieldError, DatePipe],
  styleUrl: './admin.scss',
  host: { '[attr.lang]': 'i18n.language()' },
  templateUrl: './empresa.page.html',
})
export class EmpresaPage {
  readonly i18n = inject(TranslationService);
  private readonly api = inject(AdminService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder).nonNullable;
  readonly loading = signal(false);
  readonly loaded = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly updated = signal<string | null>(null);
  readonly form = this.fb.group({
    razonSocial: ['', [notBlank, Validators.maxLength(150)]],
    ruc: ['', [Validators.required, Validators.pattern('[0-9]{11}')]],
    direccion: ['', [notBlank, Validators.maxLength(300)]],
    telefono1: ['', Validators.maxLength(15)],
    telefono2: ['', Validators.maxLength(15)],
    correo: ['', [Validators.email, Validators.maxLength(200)]],
    registroSanitario: ['', Validators.maxLength(500)],
    slogan: ['', Validators.maxLength(200)],
  });
  constructor() {
    this.load();
  }
  load() {
    if (this.loading()) return;
    this.loaded.set(false);
    this.loading.set(true);
    this.error.set('');
    this.api
      .empresa()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (empresa) => {
          this.form.reset({
            razonSocial: empresa.razonSocial,
            ruc: empresa.ruc,
            direccion: empresa.direccion,
            telefono1: empresa.telefono1 ?? '',
            telefono2: empresa.telefono2 ?? '',
            correo: empresa.correo ?? '',
            registroSanitario: empresa.registroSanitario ?? '',
            slogan: empresa.slogan ?? '',
          });
          this.updated.set(empresa.ultimaActualizacion);
          this.loaded.set(true);
        },
        error: (error) => this.error.set(this.i18n.error(error)),
      });
  }
  save() {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.saving() || this.loading() || !this.loaded()) return;
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    this.api
      .guardarEmpresa(this.form.getRawValue())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (empresa) => {
          this.updated.set(empresa.ultimaActualizacion);
          this.form.markAsPristine();
          this.success.set(this.i18n.t('admin.companyUpdated'));
        },
        error: (error) => this.error.set(this.i18n.error(error)),
      });
  }
}
