import { inject, Injectable } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { Perfil, Preferencias } from '../../core/models';

export type ActualizarPerfil = Pick<Perfil, 'nombre' | 'apellido' | 'correo' | 'telefono'>;
export type ActualizarPreferencias = Omit<Preferencias, 'usuarioId'>;
export interface CambiarContrasena {
  contrasenaActual: string;
  nuevaContrasena: string;
  confirmarContrasena: string;
}

@Injectable({ providedIn: 'root' })
export class CuentaService {
  private readonly api = inject(ApiService);
  perfil() {
    return this.api.get<Perfil>('/personal/perfil');
  }
  actualizarPerfil(datos: ActualizarPerfil) {
    return this.api.put<Perfil>('/personal/perfil', datos);
  }
  cambiarContrasena(datos: CambiarContrasena) {
    return this.api.patch<{ mensaje: string }>('/personal/perfil/contrasena', datos);
  }
  preferencias() {
    return this.api.get<Preferencias>('/personal/preferencias');
  }
  guardarPreferencias(datos: ActualizarPreferencias) {
    return this.api.put<Preferencias>('/personal/preferencias', datos);
  }
}
