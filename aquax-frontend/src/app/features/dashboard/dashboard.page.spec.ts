import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { DashboardPage } from './dashboard.page';

describe('Dashboard: errores recuperables', () => {
  it('termina la carga ante un error y permite una nueva consulta sin conservar un resumen parcial', () => {
    TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { user: () => ({ nombre: 'Ana' }) } },
      ],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    const summary = http.expectOne('/api/personal/dashboard/resumen');
    const orders = http.expectOne((r) => r.url === '/api/personal/pedidos/pagina');
    summary.flush(
      { detail: 'Servicio temporalmente no disponible.' },
      { status: 503, statusText: 'Unavailable' },
    );
    expect(orders.cancelled).toBe(true);
    expect(page.loading()).toBe(false);
    expect(page.summary()).toBeNull();
    expect(page.error()).toContain('temporalmente');
    page.load();
    http
      .expectOne('/api/personal/dashboard/resumen')
      .flush({ totalPedidos: 0, pendientes: 0, pagados: 0, deClientes: 0 });
    http
      .expectOne((r) => r.url === '/api/personal/pedidos/pagina')
      .flush({ content: [], page: { number: 0, size: 5, totalElements: 0, totalPages: 0 } });
    expect(page.loading()).toBe(false);
    expect(page.error()).toBe('');
    expect(page.summary()?.totalPedidos).toBe(0);
    http.verify();
  });
});
