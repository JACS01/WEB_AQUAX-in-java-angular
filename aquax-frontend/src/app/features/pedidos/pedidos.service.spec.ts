import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { PedidosService } from './pedidos.service';

describe('PedidosService', () => {
  let service: PedidosService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PedidosService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('envía la paginación, búsqueda y filtro de completados al backend', () => {
    service.listar(2, 25, 'AQX-0001', true, 'nombreCliente,asc').subscribe();
    const request = http.expectOne((req) => req.url === '/api/personal/pedidos/pagina');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('25');
    expect(request.request.params.get('texto')).toBe('AQX-0001');
    expect(request.request.params.get('completados')).toBe('true');
    expect(request.request.params.get('sort')).toBe('nombreCliente,asc');
    request.flush({ content: [], page: { number: 2, size: 25, totalElements: 0, totalPages: 0 } });
  });
  it('actualiza entrega y pago mediante rutas y cuerpos independientes', () => {
    service.entrega(7, 'EN_CAMINO').subscribe();
    const entrega = http.expectOne('/api/personal/pedidos/7/entrega');
    expect(entrega.request.method).toBe('PATCH');
    expect(entrega.request.body).toEqual({ estadoEntrega: 'EN_CAMINO' });
    entrega.flush({});
    service.pago(7, 'PAGADO').subscribe();
    const pago = http.expectOne('/api/personal/pedidos/7/pago');
    expect(pago.request.method).toBe('PATCH');
    expect(pago.request.body).toEqual({ estadoPago: 'PAGADO' });
    pago.flush({});
  });
});
