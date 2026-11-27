import { TranslationService } from '../../core/i18n/translation.service';
import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, input, output } from '@angular/core';
import { Producto } from '../../core/models';

/** Presentational child: receives a product and emits a quantity; the parent owns the cart. */
@Component({
  host: { '[attr.lang]': 'i18n.language()' },
  selector: 'app-producto-card',
  imports: [CurrencyPipe],
  templateUrl: './producto-card.html',
  styleUrl: './producto-card.scss',
})
export class ProductoCard {
  readonly i18n = inject(TranslationService);
  readonly producto = input.required<Producto>();
  readonly cantidad = input(0);
  readonly maxCantidad = input.required<number>();
  readonly indice = input(0);
  readonly cantidadChange = output<number>();
  readonly limiteExcedido = output<void>();
  readonly image = computed(() => {
    const name = this.producto()
      .nombre.normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
    if (/\bbidon\b/.test(name)) return 'img/bidon.png';
    if (/\bcaja\b/.test(name)) return 'img/caja.png';
    if (/\b(botellas?|paquete|pack)\b/.test(name)) return 'img/botellas.png';
    return null;
  });

  emitCantidad(value: number): boolean {
    if (!Number.isInteger(value) || value < 0) return false;
    if (value > this.maxCantidad()) {
      this.limiteExcedido.emit();
      return false;
    }
    this.cantidadChange.emit(value);
    return true;
  }

  changeQuantity(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.value.trim() || !this.emitCantidad(Number(input.value))) {
      input.value = String(this.cantidad());
    }
  }
}
