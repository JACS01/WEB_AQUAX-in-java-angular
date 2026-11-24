import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Params, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { Pedido } from '../../core/models';
import { TableQueryState } from '../../shared/table-query-state';
import { PedidosPage } from './pedidos.page';
import { PedidosService } from './pedidos.service';

function createPage(service: object, initial: Params = {}) {
  let params = initial;
  const changes = new BehaviorSubject(convertToParamMap(params));
  const route = {
    queryParamMap: changes,
    get snapshot() {
      return { queryParamMap: convertToParamMap(params) };
    },
  };
  const navigate = vi.fn((_c, options) => {
    params = { ...params, ...options.queryParams };
    for (const key of Object.keys(params))
      if (params[key] === null) delete params[key];
      else params[key] = String(params[key]);
    changes.next(convertToParamMap(params));
    return Promise.resolve(true);
  });
  TestBed.configureTestingModule({
    providers: [
      TableQueryState,
      { provide: PedidosService, useValue: service },
      { provide: ActivatedRoute, useValue: route },
      { provide: Router, useValue: { navigate } },
    ],
  });
  const page = TestBed.runInInjectionContext(() => new PedidosPage());
  return {
    page,
    navigate,
    routeTo: (next: Params) => {
      params = next;
      changes.next(convertToParamMap(params));
    },
  };
}
const empty = (number = 0, totalPages = 0) => ({
  content: [],
  page: { number, size: 10, totalElements: 0, totalPages },
});
describe('Pedidos URL and asynchronous lists', () => {
  it.each([
    ['entrega', 'CANCELADO'],
    ['entrega', 'PAGADO'],
    ['pago', 'ENTREGADO'],
    ['pago', ''],
  ] as const)('blocks an unsupported %s state %s before an HTTP mutation', (campo, value) => {
    const service = {
      listar: vi.fn().mockReturnValue(of(empty())),
      entrega: vi.fn(),
      pago: vi.fn(),
    };
    const { page } = createPage(service);
    const pedido = { id: 1, estadoEntrega: 'NUEVO', estadoPago: 'PENDIENTE' } as Pedido;
    const select = document.createElement('select');
    for (const state of ['NUEVO', 'PENDIENTE', value]) select.add(new Option(state, state));
    select.value = value;
    page.change(pedido, campo, { target: select } as unknown as Event);
    expect(service.entrega).not.toHaveBeenCalled();
    expect(service.pago).not.toHaveBeenCalled();
    expect(page.busyRows().size).toBe(0);
    expect(page.error()).toContain('estado válido');
    expect(select.value).toBe(campo === 'entrega' ? 'NUEVO' : 'PENDIENTE');
  });
  it('finds a completed notification order and opens its audit trail', () => {
    const pedido = {
      id: 8,
      numeroPedido: 'AQX-0008',
      estadoEntrega: 'ENTREGADO',
      estadoPago: 'PAGADO',
    } as Pedido;
    const service = {
      listar: vi
        .fn()
        .mockReturnValueOnce(of(empty()))
        .mockReturnValueOnce(of({ content: [pedido], page: { number: 0, totalPages: 1 } })),
      historial: vi.fn().mockReturnValue(of([])),
    };
    const { page } = createPage(service, { pedido: 'AQX-0008' });
    expect(service.listar).toHaveBeenNthCalledWith(
      1,
      0,
      10,
      'AQX-0008',
      false,
      'fechaRegistro,desc',
    );
    expect(service.listar).toHaveBeenNthCalledWith(
      2,
      0,
      10,
      'AQX-0008',
      true,
      'fechaRegistro,desc',
    );
    expect(page.completados()).toBe(true);
    expect(page.selected()).toEqual(pedido);
    expect(service.historial).toHaveBeenCalledWith(8);
  });
  it('keeps manual empty searches in the selected tab and restores completed state from URL', () => {
    const service = { listar: vi.fn().mockReturnValue(of(empty())) };
    const { page } = createPage(service, {
      completados: 'true',
      texto: 'Sin coincidencias',
      sort: 'nombreCliente,asc',
    });
    expect(page.completados()).toBe(true);
    expect(service.listar).toHaveBeenCalledTimes(1);
    expect(service.listar).toHaveBeenLastCalledWith(
      0,
      10,
      'Sin coincidencias',
      true,
      'nombreCliente,asc',
    );
  });
  it.each(['1', 'yes', 'TRUE', 'unexpected'])(
    'normalizes unsupported completed flag %s to false',
    (completados) => {
      const service = { listar: vi.fn().mockReturnValue(of(empty())) };
      const { page } = createPage(service, { completados });
      expect(page.completados()).toBe(false);
      expect(service.listar).toHaveBeenLastCalledWith(0, 10, '', false, 'fechaRegistro,desc');
    },
  );
  it('corrects page99 directly to last available page instead of querying 98 preceding pages', () => {
    const service = { listar: vi.fn().mockImplementation((page: number) => of(empty(page, 3))) };
    const { page, navigate } = createPage(service, { page: '99' });
    expect(navigate).toHaveBeenCalledTimes(1);
    expect(page.table.page()).toBe(2);
    expect(service.listar).toHaveBeenCalledTimes(2);
    expect(service.listar.mock.calls[1][0]).toBe(2);
  });
  it('corrects a notification link page before searching the completed tab', () => {
    const pedido = { id: 8, numeroPedido: 'AQX-0008' } as Pedido;
    const service = {
      listar: vi
        .fn()
        .mockReturnValueOnce(of(empty(99, 1)))
        .mockReturnValueOnce(of({ content: [pedido], page: { number: 0, totalPages: 1 } })),
      historial: vi.fn().mockReturnValue(of([])),
    };
    const { page } = createPage(service, { page: '99', pedido: '8' });
    expect(service.listar).toHaveBeenCalledTimes(2);
    expect(service.listar.mock.calls[1]).toEqual([0, 10, '8', false, 'fechaRegistro,desc']);
    expect(page.completados()).toBe(false);
    expect(page.selected()?.id).toBe(8);
  });
  it('cancels obsolete queries so a slow response cannot replace newer results', () => {
    const old = new Subject<any>();
    const latest = new Subject<any>();
    let cancelled = false;
    const service = {
      listar: vi
        .fn()
        .mockReturnValueOnce(
          new Observable((observer) => {
            const sub = old.subscribe(observer);
            return () => {
              cancelled = true;
              sub.unsubscribe();
            };
          }),
        )
        .mockReturnValueOnce(latest),
    };
    const { page, routeTo } = createPage(service);
    routeTo({ texto: 'nuevo' });
    expect(cancelled).toBe(true);
    latest.next({ content: [{ id: 2 }], page: { number: 0, totalPages: 1 } });
    old.next({ content: [{ id: 1 }], page: { number: 0, totalPages: 1 } });
    expect(page.data()?.content[0].id).toBe(2);
    latest.complete();
    expect(page.loading()).toBe(false);
  });
  it('recovers from a failed query when the user refreshes', () => {
    const first = new Subject<any>();
    const service = { listar: vi.fn().mockReturnValueOnce(first).mockReturnValueOnce(of(empty())) };
    const { page } = createPage(service);
    first.error(new Error('failed'));
    expect(page.error()).not.toBe('');
    expect(page.loading()).toBe(false);
    page.load();
    expect(service.listar).toHaveBeenCalledTimes(2);
    expect(page.error()).toBe('');
    expect(page.data()?.content).toEqual([]);
  });
  it('blocks duplicate state writes and restores the displayed state when the server rejects a change', () => {
    const response = new Subject<Pedido>();
    const service = {
      listar: vi.fn().mockReturnValue(of(empty())),
      pago: vi.fn().mockReturnValue(response),
    };
    const { page } = createPage(service);
    const pedido = { id: 1, estadoEntrega: 'NUEVO', estadoPago: 'PENDIENTE' } as Pedido;
    const select = document.createElement('select');
    select.add(new Option('Pendiente', 'PENDIENTE'));
    select.add(new Option('Pagado', 'PAGADO'));
    select.value = 'PAGADO';
    const event = { target: select } as unknown as Event;
    page.change(pedido, 'pago', event);
    page.change(pedido, 'pago', event);
    expect(service.pago).toHaveBeenCalledOnce();
    expect(page.busyRows().has(1)).toBe(true);
    response.error(new Error('rejected'));
    expect(pedido.estadoPago).toBe('PENDIENTE');
    expect(select.value).toBe('PENDIENTE');
    expect(page.busyRows().size).toBe(0);
    expect(page.error()).not.toBe('');
  });
});
