import { TranslationKey, TranslationService } from '../../core/i18n/translation.service';
import { Component, computed, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CurrencyPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CatalogoService } from './catalogo.service';
import { ProductoCard } from './producto-card';
import { Pedido, Producto } from '../../core/models';
import { FieldError } from '../../shared/field-error';
import { Icon } from '../../shared/icon';
import { notBlank, phoneFormat } from '../../shared/validators';

type OrderFormError =
  | { kind: 'http'; value: unknown }
  | { kind: 'validation'; key: TranslationKey; params?: Record<string, string | number> };

@Component({
  host: { '[attr.lang]': 'i18n.language()' },
  selector: 'app-pedido-form',
  imports: [CurrencyPipe, ReactiveFormsModule, RouterLink, FieldError, Icon, ProductoCard],
  templateUrl: './pedido-form.html',
  styleUrl: './pedido-form.scss',
})
export class PedidoForm implements OnInit {
  readonly i18n = inject(TranslationService);
  private readonly destroyRef = inject(DestroyRef);
  personal = input(false);
  private readonly catalogo = inject(CatalogoService);
  readonly products = signal<Producto[]>([]);
  readonly quantities = signal<Record<number, number>>({});
  readonly loading = signal(false);
  readonly busy = signal(false);
  private readonly submitError = signal<OrderFormError | null>(null);
  private readonly catalogError = signal<{ value: unknown } | null>(null);
  readonly error = computed(() => {
    const error = this.submitError();
    if (!error) return '';
    return error.kind === 'http'
      ? this.i18n.error(error.value)
      : this.i18n.t(error.key, error.params);
  });
  readonly loadError = computed(() => {
    const error = this.catalogError();
    return error ? this.i18n.error(error.value) : '';
  });
  readonly success = signal<Pedido | null>(null);
  readonly total = computed(
    () =>
      this.products().reduce(
        (sum, p) => sum + Math.round(p.precio * 100) * this.quantity(p.id),
        0,
      ) / 100,
  );
  readonly units = computed(() => Object.values(this.quantities()).reduce((a, b) => a + b, 0));
  readonly form = inject(FormBuilder).nonNullable.group({
    nombreCliente: ['', [notBlank, Validators.maxLength(150)]],
    telefonoCliente: ['', [notBlank, Validators.maxLength(15), phoneFormat]],
    direccionEntrega: ['', [notBlank, Validators.maxLength(300)]],
    observaciones: ['', Validators.maxLength(500)],
  });
  ngOnInit() {
    this.load();
  }
  load() {
    if (this.loading() || this.busy()) return;
    this.loading.set(true);
    this.catalogError.set(null);
    this.catalogo
      .obtenerProductos()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (p) => this.products.set(p),
        error: (e: unknown) => this.catalogError.set({ value: e }),
      });
  }
  quantity(id: number) {
    return this.quantities()[id] ?? 0;
  }
  setQuantity(id: number, value: number) {
    if (
      this.products().some((p) => p.id === id) &&
      Number.isInteger(value) &&
      value >= 0 &&
      value <= 9999
    )
      this.quantities.update((q) => ({ ...q, [id]: value }));
  }
  changeQuantity(id: number, e: Event) {
    const input = e.target as HTMLInputElement;
    this.setQuantity(id, Number(input.value));
    input.value = String(this.quantity(id));
  }
  submit() {
    if (this.busy() || this.success() || this.loading() || this.loadError()) return;
    if (this.form.invalid || !this.units()) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.submitError.set(null);
    const detalles = this.products()
      .filter((p) => this.quantity(p.id) > 0)
      .map((p) => ({ productoId: p.id, cantidad: this.quantity(p.id) }));
    if (
      !detalles.length ||
      detalles.some((d) => !Number.isSafeInteger(d.cantidad) || d.cantidad < 1 || d.cantidad > 9999)
    ) {
      this.busy.set(false);
      this.submitError.set({ kind: 'validation', key: 'orderForm.invalidQuantities' });
      return;
    }
    this.catalogo
      .registrarPedido(
        {
          ...this.form.getRawValue(),
          detalles,
        },
        this.personal(),
      )
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: (p) => this.success.set(p),
        error: (e: unknown) => this.submitError.set({ kind: 'http', value: e }),
      });
  }
  newOrder() {
    this.success.set(null);
    this.quantities.set({});
    this.form.reset();
    this.load();
  }
}
