import { inject, Injectable } from '@angular/core';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { DashboardResumen, Pagina, Pedido } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly api = inject(ApiService);
  cargar() {
    return forkJoin({
      summary: this.api.get<DashboardResumen>('/personal/dashboard/resumen'),
      orders: this.api.get<Pagina<Pedido>>('/personal/pedidos/pagina', {
        size: 5,
        sort: 'fechaRegistro,desc',
      }),
    });
  }
}
