import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, Observable, Subscription } from 'rxjs';
import { NotificacionesService } from './notificaciones.service';
import { Notificacion } from '../../core/models';
import { TranslationService } from '../../core/i18n/translation.service';
import { Icon } from '../../shared/icon';
@Component({
  selector: 'app-notificaciones',
  imports: [FormsModule, DatePipe, RouterLink, Icon],
  host: { '[attr.lang]': 'i18n.language()' },
  templateUrl: './notificaciones.page.html',
})
export class NotificacionesPage implements OnInit {
  readonly i18n = inject(TranslationService);
  private readonly service = inject(NotificacionesService);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  readonly items = signal<Notificacion[]>([]);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly unread = signal(false);
  ngOnInit() {
    this.load();
  }
  load() {
    this.request?.unsubscribe();
    this.loading.set(true);
    this.error.set('');
    this.request = this.service
      .listar(this.unread())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (items) => this.items.set(items),
        error: (e) => this.error.set(this.i18n.error(e)),
      });
  }
  mark(n: Notificacion) {
    this.change(this.service.marcarLeida(n.id));
  }
  markAll() {
    this.change(this.service.marcarTodas());
  }
  notificationTitle(notification: Notificacion): string {
    return notification.tipo === 'pedido_nuevo'
      ? this.i18n.t('notifications.newCustomerOrder')
      : notification.titulo;
  }
  notificationMessage(notification: Notificacion): string {
    // This known server template contains a customer name; preserve that data exactly.
    const prefix = `Nuevo pedido ${notification.numeroPedido} de `;
    if (
      notification.tipo === 'pedido_nuevo' &&
      notification.mensaje.startsWith(prefix) &&
      notification.mensaje.endsWith('.')
    ) {
      return this.i18n.t('notifications.newOrderMessage', {
        order: notification.numeroPedido,
        customer: notification.mensaje.slice(prefix.length, -1),
      });
    }
    return notification.mensaje;
  }
  private change(request: Observable<unknown>) {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    request
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({ next: () => this.load(), error: (e) => this.error.set(this.i18n.error(e)) });
  }
}
