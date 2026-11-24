import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, Subscription } from 'rxjs';
import { HistorialPagados, ReporteCompletados } from '../../core/models';
import { TranslationService } from '../../core/i18n/translation.service';
import { FiltrosReporte, ReportesService } from './reportes.service';
import { nombreCsv, rangoValido } from './reportes.utils';

@Component({
  selector: 'app-reportes-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  host: { '[attr.lang]': 'i18n.language()' },
  templateUrl: './reportes.page.html',
  styles: [
    `
      :host {
        display: block;
      }
      .card {
        padding: 24px;
      }
      .filters {
        display: flex;
        gap: 16px;
        align-items: end;
        flex-wrap: wrap;
      }
      .filters label {
        min-width: 170px;
      }
      .filters .actions {
        margin-left: auto;
      }
      .report-intro {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 16px;
        flex-wrap: wrap;
        margin: 22px 0;
      }
      .report-intro h2 {
        margin: 0;
      }
      .product-summary {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
        gap: 12px;
        margin: 20px 0;
      }
      .product-summary article {
        padding: 18px;
        background: var(--background, #f4f8f9);
        border-radius: 12px;
      }
      .product-summary strong {
        display: block;
        font-size: 1.6rem;
        margin-top: 10px;
      }
      .report-orders {
        margin-top: 24px;
      }
      .amount {
        font-size: 2rem;
        font-weight: 750;
        margin: 10px 0;
      }
      .table-wrap {
        margin-top: 16px;
      }
      .section-note {
        font-size: 0.85rem;
      }
      .sr-only {
        position: absolute;
        width: 1px;
        height: 1px;
        overflow: hidden;
        clip: rect(0, 0, 0, 0);
      }
      @media (max-width: 600px) {
        .card {
          padding: 18px;
        }
        .filters label {
          width: 100%;
        }
        .filters .actions {
          margin-left: 0;
          width: 100%;
          flex-wrap: wrap;
        }
        .amount {
          font-size: 1.5rem;
          overflow-wrap: anywhere;
        }
      }
    `,
  ],
})
export class ReportesPage {
  readonly i18n = inject(TranslationService);
  private readonly service = inject(ReportesService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private request?: Subscription;
  readonly tab = signal<'completados' | 'pagados'>('completados');
  readonly report = signal<ReporteCompletados | null>(null);
  readonly paid = signal<HistorialPagados | null>(null);
  readonly loading = signal(false);
  readonly downloading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly dateError = signal('');
  readonly appliedFilters = signal<FiltrosReporte>({});
  readonly form = this.fb.nonNullable.group({ desde: '', hasta: '' });

  constructor() {
    this.load();
    this.destroyRef.onDestroy(() => this.request?.unsubscribe());
  }
  switchTab(tab: 'completados' | 'pagados') {
    this.tab.set(tab);
    this.error.set('');
    this.load();
  }
  apply() {
    const { desde, hasta } = this.form.getRawValue();
    if (!rangoValido(desde, hasta)) {
      this.dateError.set(this.i18n.t('reports.invalidDates'));
      return;
    }
    this.dateError.set('');
    const filters: FiltrosReporte = {};
    if (desde) filters['desde'] = desde;
    if (hasta) filters['hasta'] = hasta;
    this.appliedFilters.set(filters);
    this.load();
  }
  clear() {
    this.form.reset();
    this.apply();
  }
  load() {
    this.request?.unsubscribe();
    this.loading.set(true);
    this.error.set('');
    this.success.set('');
    if (this.tab() === 'completados') {
      this.report.set(null);
      this.request = this.service
        .completados(this.appliedFilters())
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: (report) => this.report.set(report),
          error: (error) => this.error.set(this.i18n.error(error)),
        });
    } else {
      this.paid.set(null);
      this.request = this.service
        .pagados()
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: (paid) => this.paid.set(paid),
          error: (error) => this.error.set(this.i18n.error(error)),
        });
    }
  }
  download() {
    if (this.downloading() || !this.report()) return;
    this.downloading.set(true);
    this.error.set('');
    this.success.set('');
    this.service
      .csv(this.appliedFilters())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.downloading.set(false)),
      )
      .subscribe({
        next: (response) => {
          if (!response.body) {
            this.error.set(this.i18n.t('reports.emptyFile'));
            return;
          }
          const url = URL.createObjectURL(response.body);
          const link = document.createElement('a');
          link.href = url;
          link.download = nombreCsv(response.headers.get('Content-Disposition'));
          document.body.appendChild(link);
          link.click();
          link.remove();
          setTimeout(() => URL.revokeObjectURL(url), 1000);
          this.success.set(this.i18n.t('reports.downloaded'));
        },
        error: async (error) => {
          if (error instanceof HttpErrorResponse && error.error instanceof Blob) {
            try {
              const detail = JSON.parse(await error.error.text());
              this.error.set(
                this.i18n.error(new HttpErrorResponse({ error: detail, status: error.status })),
              );
            } catch {
              this.error.set(this.i18n.error(error));
            }
          } else {
            this.error.set(this.i18n.error(error));
          }
        },
      });
  }
}
