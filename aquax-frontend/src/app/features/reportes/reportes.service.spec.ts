import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ReportesService } from './reportes.service';

describe('ReportesService', () => {
  let service: ReportesService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReportesService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('descarga el CSV como blob y conserva las cabeceras para el nombre del archivo', () => {
    const blob = new Blob(['pedido,cliente'], { type: 'text/csv' });
    service.csv({ desde: '2026-11-01', hasta: '2026-11-11' }).subscribe((response) => {
      expect(response.body).toBe(blob);
      expect(response.headers.get('Content-Disposition')).toContain('historial.csv');
    });
    const request = http.expectOne((req) => req.url === '/api/personal/reportes/completados/csv');
    expect(request.request.responseType).toBe('blob');
    expect(request.request.params.get('desde')).toBe('2026-11-01');
    expect(request.request.params.get('hasta')).toBe('2026-11-11');
    request.flush(blob, {
      headers: { 'Content-Disposition': 'attachment; filename="historial.csv"' },
    });
  });
});
