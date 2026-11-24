import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { DashboardService } from './dashboard.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth/auth.service';
import { DashboardResumen, Pedido } from '../../core/models';
import { TranslationService } from '../../core/i18n/translation.service';
import { Icon } from '../../shared/icon';
@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, CurrencyPipe, DatePipe, Icon],
  host: { '[attr.lang]': 'i18n.language()' },
  templateUrl: './dashboard.page.html',
})
export class DashboardPage implements OnInit {
  readonly i18n = inject(TranslationService);
  readonly auth = inject(AuthService);
  private readonly service = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);
  readonly summary = signal<DashboardResumen | null>(null);
  readonly orders = signal<Pedido[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  ngOnInit() {
    this.load();
  }
  load() {
    this.loading.set(true);
    this.error.set('');
    this.service
      .cargar()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (r) => {
          this.summary.set(r.summary);
          this.orders.set(r.orders.content);
        },
        error: (e) => this.error.set(this.i18n.error(e)),
      });
  }
}
