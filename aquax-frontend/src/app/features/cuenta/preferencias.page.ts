import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { CuentaService } from './cuenta.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PreferencesService } from '../../core/preferences.service';
import { TranslationKey, TranslationService } from '../../core/i18n/translation.service';
import { oneOf } from '../../shared/validators';
@Component({
  selector: 'app-preferencias',
  imports: [ReactiveFormsModule],
  templateUrl: './preferencias.page.html',
  host: { '[attr.lang]': 'i18n.language()' },
})
export class PreferenciasPage implements OnInit {
  private readonly service = inject(CuentaService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly prefs = inject(PreferencesService);
  readonly i18n = inject(TranslationService);
  readonly levels = [80, 90, 100, 110, 120, 130, 150];
  readonly filters = [
    { value: 'none', label: 'preferences.filters.none' },
    { value: 'deuteranopia', label: 'preferences.filters.deuteranopia' },
    { value: 'protanopia', label: 'preferences.filters.protanopia' },
    { value: 'tritanopia', label: 'preferences.filters.tritanopia' },
    { value: 'grayscale', label: 'preferences.filters.grayscale' },
    { value: 'contrast', label: 'preferences.filters.contrast' },
    { value: 'warm', label: 'preferences.filters.warm' },
    { value: 'cool', label: 'preferences.filters.cool' },
    { value: 'sepia', label: 'preferences.filters.sepia' },
    { value: 'invert', label: 'preferences.filters.invert' },
  ] satisfies { value: string; label: TranslationKey }[];
  readonly loading = signal(false);
  readonly loaded = signal(false);
  readonly busy = signal(false);
  private readonly errorKey = signal<TranslationKey | ''>('');
  readonly error = computed(() => {
    const key = this.errorKey();
    return key ? this.i18n.t(key) : '';
  });
  readonly saved = signal(false);
  readonly form = inject(FormBuilder).nonNullable.group({
    idioma: ['es', [Validators.required, oneOf(['es', 'en', 'pt'])]],
    nivelZoom: [100, [Validators.required, oneOf(this.levels)]],
    filtroColor: ['none', [Validators.required, oneOf(this.filters.map((f) => f.value))]],
    filtroColorActivo: [false, oneOf([true, false])],
  });
  ngOnInit() {
    this.load();
  }
  load() {
    if (this.loading() || this.busy()) return;
    this.loading.set(true);
    this.loaded.set(false);
    this.errorKey.set('');
    this.service
      .preferencias()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (p) => {
          this.form.patchValue(p);
          this.prefs.apply(p);
          this.loaded.set(true);
        },
        error: (e: unknown) => this.errorKey.set(this.errorTranslationKey(e)),
      });
  }
  reset() {
    this.form.reset({
      idioma: 'es',
      nivelZoom: 100,
      filtroColor: 'none',
      filtroColorActivo: false,
    });
    this.saved.set(false);
  }
  save() {
    if (this.busy() || this.loading() || !this.loaded()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorKey.set('preferences.invalid');
      return;
    }
    this.busy.set(true);
    this.errorKey.set('');
    this.saved.set(false);
    this.service
      .guardarPreferencias(this.form.getRawValue())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: (p) => {
          this.form.patchValue(p);
          this.prefs.apply(p);
          this.saved.set(true);
        },
        error: (e: unknown) => this.errorKey.set(this.errorTranslationKey(e)),
      });
  }
  private errorTranslationKey(error: unknown): TranslationKey {
    if (!(error instanceof HttpErrorResponse)) return 'errors.generic';
    const keys: Record<number, TranslationKey> = {
      0: 'errors.network',
      400: 'errors.badRequest',
      401: 'errors.unauthorized',
      403: 'errors.forbidden',
      404: 'errors.notFound',
      409: 'errors.conflict',
      503: 'errors.unavailable',
    };
    return keys[error.status] ?? 'errors.generic';
  }
}
