import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, EMPTY, finalize, map, Subscription, switchMap } from 'rxjs';
import {
  EstadoEntrega,
  EstadoPago,
  HistorialModificacion,
  Pagina,
  Pedido,
} from '../../core/models';
import { TranslationService } from '../../core/i18n/translation.service';
import { PedidosService } from './pedidos.service';
import { TableQueryState } from '../../shared/table-query-state';

@Component({
  selector: 'app-pedidos-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  providers: [TableQueryState],
  host: { '[attr.lang]': 'i18n.language()' },
  templateUrl: './pedidos.page.html',
  styles: [
    `
      :host {
        display: block;
      }
      .order-code {
        font-weight: 750;
        white-space: nowrap;
      }
      .customer small {
        display: block;
        margin-top: 4px;
      }
      .status-select {
        min-width: 120px;
        padding: 7px;
        border: 1px solid var(--border, #dce5e8);
        border-radius: 8px;
        background: var(--surface, #fff);
        font: inherit;
        font-size: 0.82rem;
      }
      .detail-header {
        display: flex;
        justify-content: space-between;
        align-items: start;
        gap: 16px;
      }
      .detail-section {
        margin-top: 24px;
      }
      .timeline {
        list-style: none;
        padding: 0;
        display: grid;
        gap: 16px;
      }
      .timeline li {
        border-left: 3px solid #0b9c91;
        padding-left: 16px;
      }
      .timeline p {
        margin: 4px 0;
      }
      .timeline small {
        color: var(--muted, #60737b);
      }
      .detail-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
        gap: 16px;
      }
      .detail-grid dt {
        font-size: 0.8rem;
        color: var(--muted, #60737b);
        margin-bottom: 5px;
      }
      .detail-grid dd {
        margin: 0;
        overflow-wrap: anywhere;
      }
      .table-wrap {
        margin-top: 12px;
      }
      .result-count {
        margin: 0;
      }
      .toolbar form {
        display: flex;
        gap: 10px;
        flex: 1;
        flex-wrap: wrap;
      }
      .toolbar input {
        min-width: 180px;
        flex: 1;
      }
      .detail-card {
        margin-top: 24px;
        padding: 24px;
      }
      .sr-only {
        position: absolute;
        width: 1px;
        height: 1px;
        overflow: hidden;
        clip: rect(0, 0, 0, 0);
      }
      @media (max-width: 600px) {
        .detail-card {
          padding: 18px;
        }
        .detail-header {
          flex-wrap: wrap;
        }
        .detail-grid {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class PedidosPage {
  readonly i18n = inject(TranslationService);
  private readonly service = inject(PedidosService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  readonly table = inject(TableQueryState);
  private readonly router = inject(Router);
  private historyRequest?: Subscription;
  private openedDeepLink = '';
  private criteriaKey = '';
  readonly data = signal<Pagina<Pedido> | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly completados = signal(false);
  readonly busyRows = signal<Set<number>>(new Set());
  readonly selected = signal<Pedido | null>(null);
  readonly history = signal<HistorialModificacion[]>([]);
  readonly historyLoading = signal(false);
  readonly historyError = signal('');
  constructor() {
    this.table.initialize({
      defaultSort: 'fechaRegistro,desc',
      allowedSortFields: [
        'id',
        'numeroPedido',
        'nombreCliente',
        'telefonoCliente',
        'fechaRegistro',
        'fechaEntrega',
        'estadoEntrega',
        'estadoPago',
        'origen',
      ],
      searchResetParams: ['pedido'],
      searchFallbackParam: 'pedido',
    });
    this.table.changes$
      .pipe(
        switchMap((query) => {
          const params = this.table.params();
          const completed = params.get('completados') === 'true';
          const deepLink = params.get('pedido') || '';
          const texto = query.texto;
          this.completados.set(completed);
          this.loading.set(true);
          this.error.set('');
          const key = JSON.stringify([query, completed, deepLink]);
          if (key !== this.criteriaKey) {
            this.closeDetail();
            this.openedDeepLink = '';
            this.criteriaKey = key;
          }
          return this.service.listar(query.page, query.size, texto, completed, query.sort).pipe(
            map((data) => ({ data, query, deepLink, completed })),
            catchError((error) => {
              this.data.set(null);
              this.error.set(this.i18n.error(error));
              return EMPTY;
            }),
            finalize(() => this.loading.set(false)),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ data, query, deepLink, completed }) => {
        const lastPage = Math.max(data.page.totalPages - 1, 0);
        if (query.page > lastPage) {
          this.table.goTo(lastPage);
          return;
        }
        if (deepLink && !completed && !data.content.length) {
          void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { completados: true, page: 0 },
            queryParamsHandling: 'merge',
            replaceUrl: true,
          });
          return;
        }
        this.data.set(data);
        if (deepLink && deepLink !== this.openedDeepLink) {
          const code = deepLink.toUpperCase();
          const pedido = data.content.find(
            (item) => item.numeroPedido === code || String(item.id) === code,
          );
          if (pedido) {
            this.openedDeepLink = deepLink;
            this.details(pedido);
          }
        }
      });
    this.destroyRef.onDestroy(() => this.historyRequest?.unsubscribe());
  }
  load() {
    this.table.refresh();
  }
  search() {
    this.openedDeepLink = '';
    this.table.searchNow();
  }
  tab(completados: boolean) {
    this.openedDeepLink = '';
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { completados, page: 0, pedido: null },
      queryParamsHandling: 'merge',
    });
  }
  paginate(page: number) {
    this.table.goTo(page);
  }
  closeDetail() {
    this.historyRequest?.unsubscribe();
    this.selected.set(null);
  }
  details(pedido: Pedido) {
    this.historyRequest?.unsubscribe();
    this.selected.set(pedido);
    this.history.set([]);
    this.historyError.set('');
    this.historyLoading.set(true);
    this.historyRequest = this.service
      .historial(pedido.id)
      .pipe(finalize(() => this.historyLoading.set(false)))
      .subscribe({
        next: (history) => this.history.set(history),
        error: (error) => this.historyError.set(this.i18n.error(error)),
      });
  }
  change(pedido: Pedido, campo: 'entrega' | 'pago', event: Event) {
    const select = event.target as HTMLSelectElement;
    const previous = campo === 'entrega' ? pedido.estadoEntrega : pedido.estadoPago;
    const value = select.value;
    const allowed =
      campo === 'entrega' ? ['NUEVO', 'EN_CAMINO', 'ENTREGADO'] : ['PENDIENTE', 'PAGADO'];
    if (!allowed.includes(value)) {
      select.value = previous;
      this.error.set(this.i18n.t('orders.invalidStatus'));
      return;
    }
    if (previous === value || this.busyRows().has(pedido.id)) return;
    this.busyRows.update((ids) => new Set([...ids, pedido.id]));
    this.error.set('');
    this.success.set('');
    const request =
      campo === 'entrega'
        ? this.service.entrega(pedido.id, value as EstadoEntrega)
        : this.service.pago(pedido.id, value as EstadoPago);
    request
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() =>
          this.busyRows.update((ids) => {
            const next = new Set(ids);
            next.delete(pedido.id);
            return next;
          }),
        ),
      )
      .subscribe({
        next: (updated) => {
          this.success.set(
            this.i18n.t(campo === 'entrega' ? 'orders.deliveryUpdated' : 'orders.paymentUpdated', {
              order: updated.numeroPedido,
            }),
          );
          if (this.selected()?.id === updated.id) this.details(updated);
          this.load();
        },
        error: (error) => {
          select.value = previous;
          this.error.set(this.i18n.error(error));
        },
      });
  }
  label(value: string) {
    return this.i18n.label(value);
  }
}
