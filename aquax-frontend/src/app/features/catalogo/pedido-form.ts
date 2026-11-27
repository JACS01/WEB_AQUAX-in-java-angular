import { TranslationKey, TranslationService } from '../../core/i18n/translation.service';
import {
  afterNextRender,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  Injector,
  input,
  OnInit,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CurrencyPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CatalogoService } from './catalogo.service';
import { ProductoCard } from './producto-card';
import { CrearPedido, Pedido, Producto } from '../../core/models';
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
  private readonly injector = inject(Injector);
  private readonly catalogHeading = viewChild<ElementRef<HTMLElement>>('catalogHeading');
  private readonly cartHeading = viewChild<ElementRef<HTMLElement>>('cartHeading');
  private readonly deliveryHeading = viewChild<ElementRef<HTMLElement>>('deliveryHeading');
  private readonly reviewHeading = viewChild<ElementRef<HTMLElement>>('reviewHeading');
  private readonly confirmationHeading = viewChild<ElementRef<HTMLElement>>('confirmationHeading');
  personal = input(false);
  private readonly catalogo = inject(CatalogoService);
  readonly products = signal<Producto[]>([]);
  readonly quantities = signal<Record<number, number>>({});
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly reviewing = signal(false);
  readonly orderLimit = 30;
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
  readonly cart = computed(() =>
    this.products()
      .map((product) => {
        const quantity = this.quantity(product.id);
        const subtotalCents = Math.round(product.precio * 100) * quantity;
        return { product, quantity, subtotalCents };
      })
      .filter((line) => line.quantity > 0),
  );
  readonly total = computed(
    () => this.cart().reduce((sum, line) => sum + line.subtotalCents, 0) / 100,
  );
  readonly units = computed(() => this.cart().reduce((sum, line) => sum + line.quantity, 0));
  readonly canContinue = computed(
    () =>
      !this.busy() &&
      !this.loading() &&
      !this.catalogError() &&
      this.units() > 0 &&
      this.units() <= this.orderLimit,
  );
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
  quantityLimit(id: number) {
    return Math.max(0, this.orderLimit - this.units() + this.quantity(id));
  }
  showOrderLimit() {
    this.submitError.set({
      kind: 'validation',
      key: 'orderForm.orderLimit',
      params: { limit: this.orderLimit },
    });
  }
  setQuantity(id: number, value: number) {
    if (
      this.busy() ||
      !this.products().some((p) => p.id === id) ||
      !Number.isInteger(value) ||
      value < 0 ||
      value === this.quantity(id)
    )
      return;
    if (value > this.quantityLimit(id)) {
      this.showOrderLimit();
      return;
    }
    this.quantities.update((q) => ({ ...q, [id]: value }));
    this.submitError.set(null);
  }
  changeQuantity(id: number, e: Event) {
    const input = e.target as HTMLInputElement;
    if (input.value.trim()) {
      const quantity = Number(input.value);
      if (quantity === 0) this.removeFromCart(id);
      else this.setQuantity(id, quantity);
    }
    input.value = String(this.quantity(id));
  }
  removeFromCart(id: number) {
    this.setQuantity(id, 0);
    this.focusAfterRender(this.cartHeading);
  }
  reviewOrder() {
    if (!this.validatedOrder()) return;
    this.submitError.set(null);
    this.reviewing.set(true);
    this.focusAfterRender(this.reviewHeading);
  }
  editOrder(target: 'delivery' | 'products') {
    if (this.busy()) return;
    this.submitError.set(null);
    this.reviewing.set(false);
    this.focusAfterRender(target === 'delivery' ? this.deliveryHeading : this.catalogHeading);
  }
  private validatedOrder(): CrearPedido | null {
    if (this.success() || this.busy() || this.loading() || this.catalogError()) return null;
    if (this.form.invalid || !this.cart().length) {
      this.form.markAllAsTouched();
      return null;
    }
    const detalles = this.cart().map(({ product, quantity }) => ({
      productoId: product.id,
      cantidad: quantity,
    }));
    if (detalles.some(({ cantidad }) => !Number.isInteger(cantidad))) {
      this.submitError.set({ kind: 'validation', key: 'orderForm.invalidQuantities' });
      return null;
    }
    if (this.units() > this.orderLimit) {
      this.showOrderLimit();
      return null;
    }
    return { ...this.form.getRawValue(), detalles };
  }
  submit() {
    if (!this.reviewing()) return;
    const order = this.validatedOrder();
    if (!order) return;
    this.busy.set(true);
    this.submitError.set(null);
    this.catalogo
      .registrarPedido(order, this.personal())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: (p) => {
          this.success.set(p);
          this.focusAfterRender(this.confirmationHeading);
        },
        error: (e: unknown) => this.submitError.set({ kind: 'http', value: e }),
      });
  }
  newOrder() {
    this.success.set(null);
    this.reviewing.set(false);
    this.quantities.set({});
    this.form.reset();
    this.load();
    this.focusAfterRender(this.catalogHeading);
  }
  private focusAfterRender(target: () => ElementRef<HTMLElement> | undefined) {
    afterNextRender(() => target()?.nativeElement.focus(), { injector: this.injector });
  }
}
