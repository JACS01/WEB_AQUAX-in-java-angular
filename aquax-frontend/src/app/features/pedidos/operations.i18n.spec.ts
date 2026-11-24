import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { TranslationService } from '../../core/i18n/translation.service';
import { Notificacion, Pedido } from '../../core/models';
import { DashboardPage } from '../dashboard/dashboard.page';
import { DashboardService } from '../dashboard/dashboard.service';
import { NotificacionesPage } from '../notificaciones/notificaciones.page';
import { NotificacionesService } from '../notificaciones/notificaciones.service';
import { ReportesPage } from '../reportes/reportes.page';
import { ReportesService } from '../reportes/reportes.service';
import { PedidosPage } from './pedidos.page';
import { PedidosService } from './pedidos.service';

registerLocaleData(localePt);
const customer = 'José "Orders" da Silva';
const order: Pedido = {
  id: 8,
  numeroPedido: 'AQX-0008',
  nombreCliente: customer,
  telefonoCliente: '999888777',
  direccionEntrega: 'Av. Los Álamos 123',
  observaciones: 'Entregar agua fría',
  fechaRegistro: '2026-11-18',
  fechaEntrega: null,
  origen: 'CLIENTE',
  estadoEntrega: 'NUEVO',
  estadoPago: 'PENDIENTE',
  cantidadProducto: 1,
  total: 10,
  detalles: [],
  usuarioRegistroId: null,
  usuarioRegistro: null,
};
const notification: Notificacion = {
  id: 1,
  pedidoId: 8,
  numeroPedido: 'AQX-0008',
  titulo: 'Nuevo pedido de cliente',
  mensaje: `Nuevo pedido AQX-0008 de ${customer}.`,
  tipo: 'pedido_nuevo',
  leida: false,
  fechaCreacion: '2026-11-18T12:00:00-05:00',
};

describe('Operaciones: traducciones y conservación de datos', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DashboardPage, PedidosPage, ReportesPage, NotificacionesPage],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { user: () => ({ nombre: customer }) } },
        {
          provide: DashboardService,
          useValue: {
            cargar: () =>
              of({
                summary: { totalPedidos: 1, pendientes: 1, pagados: 0, deClientes: 1 },
                orders: { content: [order] },
              }),
          },
        },
        {
          provide: PedidosService,
          useValue: {
            listar: () =>
              of({
                content: [order],
                page: { number: 0, size: 10, totalElements: 1, totalPages: 1 },
              }),
            historial: () => of([]),
          },
        },
        {
          provide: ReportesService,
          useValue: {
            completados: () =>
              of({ desde: null, hasta: null, totalPedidos: 0, productos: [], pedidos: [] }),
          },
        },
        { provide: NotificacionesService, useValue: { listar: () => of([notification]) } },
      ],
    });
  });

  it('traduce las cuatro pantallas sin convertir clientes, direcciones o valores del contrato', () => {
    const i18n = TestBed.inject(TranslationService);
    i18n.use('en');
    const dashboard = TestBed.createComponent(DashboardPage);
    const orders = TestBed.createComponent(PedidosPage);
    const reports = TestBed.createComponent(ReportesPage);
    const notifications = TestBed.createComponent(NotificacionesPage);
    for (const fixture of [dashboard, orders, reports, notifications]) fixture.detectChanges();
    expect(dashboard.nativeElement.querySelector('h1').textContent).toContain(`Hello, ${customer}`);
    expect(orders.nativeElement.querySelector('h1').textContent).toBe('Orders');
    expect(reports.nativeElement.querySelector('h1').textContent).toBe('Reports and history');
    expect(notifications.nativeElement.querySelector('h1').textContent).toBe('Notifications');
    const status: HTMLSelectElement = orders.nativeElement.querySelector('.status-select');
    expect(status.value).toBe('NUEVO');
    expect(status.options[0].textContent).toBe('New');
    expect(orders.nativeElement.textContent).toContain(customer);
    orders.componentInstance.details(order);
    orders.detectChanges();
    expect(orders.nativeElement.textContent).toContain(order.direccionEntrega);
    expect(orders.nativeElement.textContent).toContain(order.observaciones);
    expect(notifications.nativeElement.textContent).toContain(
      `New order AQX-0008 from ${customer}.`,
    );

    i18n.use('pt');
    for (const fixture of [dashboard, orders, reports, notifications]) fixture.detectChanges();
    expect(reports.nativeElement.querySelector('h1').textContent).toBe('Relatórios e histórico');
    expect(notifications.nativeElement.querySelector('h1').textContent).toBe('Notificações');
    expect(status.value).toBe('NUEVO');
    expect(status.options[0].textContent).toBe('Novo');
    expect(notifications.nativeElement.textContent).toContain(
      `Novo pedido AQX-0008 de ${customer}.`,
    );
  });

  it('usa singular para un pedido y plural para varios', () => {
    const fixture = TestBed.createComponent(PedidosPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.result-count').textContent).toContain(
      '1 pedido ·',
    );

    const orders = fixture.componentInstance;
    const response = orders.data()!;
    orders.data.set({ ...response, page: { ...response.page, totalElements: 2 } });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.result-count').textContent).toContain(
      '2 pedidos ·',
    );
  });

  it('conserva mensajes desconocidos y no intenta traducir contenido libre', () => {
    const page = TestBed.createComponent(NotificacionesPage).componentInstance;
    TestBed.inject(TranslationService).use('en');
    const custom = {
      ...notification,
      tipo: 'personalizada',
      titulo: 'Hola Ana',
      mensaje: 'Entregar mañana.',
    };
    expect(page.notificationTitle(custom)).toBe(custom.titulo);
    expect(page.notificationMessage(custom)).toBe(custom.mensaje);
    const unexpectedTemplate = { ...notification, mensaje: 'Anotación libre del cliente.' };
    expect(page.notificationMessage(unexpectedTemplate)).toBe(unexpectedTemplate.mensaje);
  });
});
