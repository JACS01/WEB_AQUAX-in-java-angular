import { inject, Injectable } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { Notificacion } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class NotificacionesService {
  private readonly api = inject(ApiService);
  listar(soloNoLeidas: boolean) {
    return this.api.get<Notificacion[]>('/personal/notificaciones', { soloNoLeidas });
  }
  marcarLeida(id: number) {
    return this.api.patch<Notificacion>(`/personal/notificaciones/${id}/leida`, {});
  }
  marcarTodas() {
    return this.api.patch<{ actualizadas: number }>('/personal/notificaciones/leidas', {});
  }
}
