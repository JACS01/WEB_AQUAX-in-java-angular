import { inject, Injectable } from '@angular/core';
import { ApiService } from '../../core/api.service';
import {
  EstadoEntrega,
  EstadoPago,
  HistorialModificacion,
  Pagina,
  Pedido,
} from '../../core/models';

@Injectable({ providedIn: 'root' })
export class PedidosService {
  private readonly api = inject(ApiService);
  listar(
    page: number,
    size: number,
    texto: string,
    completados: boolean,
    sort = 'fechaRegistro,desc',
  ) {
    return this.api.get<Pagina<Pedido>>('/personal/pedidos/pagina', {
      page,
      size,
      texto,
      completados,
      sort,
    });
  }
  historial(id: number) {
    return this.api.get<HistorialModificacion[]>(`/personal/pedidos/${id}/historial`);
  }
  entrega(id: number, estadoEntrega: EstadoEntrega) {
    return this.api.patch<Pedido>(`/personal/pedidos/${id}/entrega`, { estadoEntrega });
  }
  pago(id: number, estadoPago: EstadoPago) {
    return this.api.patch<Pedido>(`/personal/pedidos/${id}/pago`, { estadoPago });
  }
}
