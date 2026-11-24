import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { CrearPedido, Pedido, Producto } from '../../core/models';

/** Data service: components describe the interaction; this service owns the API contract. */
@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly api = inject(ApiService);

  obtenerProductos(): Observable<Producto[]> {
    return this.api.get<Producto[]>('/productos');
  }

  registrarPedido(solicitud: CrearPedido, personal = false): Observable<Pedido> {
    return this.api.post<Pedido>(personal ? '/personal/pedidos' : '/pedidos', solicitud);
  }
}
