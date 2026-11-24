import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { HistorialPagados, ReporteCompletados } from '../../core/models';
import { environment } from '../../../environments/environment';

export type FiltrosReporte = Record<string, string>;

@Injectable({ providedIn: 'root' })
export class ReportesService {
  private readonly api = inject(ApiService);
  private readonly http = inject(HttpClient);
  completados(filters: FiltrosReporte) {
    return this.api.get<ReporteCompletados>('/personal/reportes/completados', filters);
  }
  pagados() {
    return this.api.get<HistorialPagados>('/personal/pedidos/pagados');
  }
  csv(filters: FiltrosReporte) {
    return this.http.get(`${environment.apiUrl}/personal/reportes/completados/csv`, {
      params: new HttpParams({ fromObject: filters }),
      observe: 'response',
      responseType: 'blob',
    });
  }
}
