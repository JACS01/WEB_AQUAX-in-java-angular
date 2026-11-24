import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { ReportesPage } from './reportes.page';
import { ReportesService } from './reportes.service';

describe('Reportes: validación del formulario y escrituras de archivo', () => {
  const empty = { desde: null, hasta: null, totalPedidos: 0, productos: [], pedidos: [] };
  const service = { completados: vi.fn(), pagados: vi.fn(), csv: vi.fn() };
  beforeEach(() => {
    vi.resetAllMocks();
    service.completados.mockReturnValue(of(empty));
    service.pagados.mockReturnValue(of({ totalPagados: 0, montoTotal: 0, pedidos: [] }));
    TestBed.configureTestingModule({
      providers: [{ provide: ReportesService, useValue: service }],
    });
  });
  function page() {
    return TestBed.runInInjectionContext(() => new ReportesPage());
  }

  it.each([
    ['2026-11-12', '2026-11-11'],
    ['2026-02-30', ''],
    ['', '2025-02-29'],
    ['11/11/2026', ''],
  ])('no consulta la API si el rango %s - %s es inválido', (desde, hasta) => {
    const component = page();
    service.completados.mockClear();
    component.form.setValue({ desde, hasta });
    component.apply();
    expect(service.completados).not.toHaveBeenCalled();
    expect(component.dateError()).not.toBe('');
    expect(component.appliedFilters()).toEqual({});
  });

  it('usa los filtros confirmados para descargar y bloquea descargas duplicadas', () => {
    const component = page();
    component.form.setValue({ desde: '2024-02-29', hasta: '2024-03-01' });
    component.apply();
    expect(service.completados).toHaveBeenLastCalledWith({
      desde: '2024-02-29',
      hasta: '2024-03-01',
    });
    // Editing without Aplicar does not change the dataset currently shown/downloaded.
    component.form.controls.hasta.setValue('2024-04-01');
    const response = new Subject<HttpResponse<Blob>>();
    service.csv.mockReturnValue(response);
    component.download();
    component.download();
    expect(service.csv).toHaveBeenCalledOnce();
    expect(service.csv).toHaveBeenLastCalledWith({ desde: '2024-02-29', hasta: '2024-03-01' });
    response.error(
      new HttpErrorResponse({
        status: 503,
        error: { detail: 'Servidor temporalmente no disponible.' },
      }),
    );
    expect(component.downloading()).toBe(false);
    expect(component.error()).toContain('temporalmente');
  });

  it('impide descargar cuando todavía no terminó la consulta', () => {
    service.completados.mockReturnValue(new Subject());
    const component = page();
    component.download();
    expect(component.loading()).toBe(true);
    expect(component.report()).toBeNull();
    expect(service.csv).not.toHaveBeenCalled();
  });
});
