import { inject, Injectable } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { CrearUsuario, Empleado, Empresa, Pagina, RolUsuario, Usuario } from '../../core/models';

export interface DatosPerfil {
  nombre: string;
  apellido: string;
  correo: string;
  telefono: string;
}
export interface DatosLaborales {
  dni: string;
  fechaIngreso: string;
}
export interface DatosAltaLaboral {
  dni: string;
  fechaIngreso?: string;
}
export type DatosEmpresa = Pick<
  Empresa,
  | 'razonSocial'
  | 'ruc'
  | 'direccion'
  | 'telefono1'
  | 'telefono2'
  | 'correo'
  | 'registroSanitario'
  | 'slogan'
>;

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly api = inject(ApiService);
  usuarios(page = 0, texto = '', size = 10, sort = 'nombre,asc') {
    return this.api.get<Pagina<Usuario>>('/admin/usuarios/pagina', {
      page,
      size,
      texto,
      sort,
    });
  }
  cuentas() {
    return this.api.get<Usuario[]>('/admin/usuarios');
  }
  crearUsuario(datos: CrearUsuario) {
    return this.api.post<Usuario>('/admin/usuarios', datos);
  }
  activarUsuario(id: number, activo: boolean) {
    return this.api.patch<Usuario>(`/admin/usuarios/${id}/activo`, { activo });
  }
  empleados(page = 0, texto = '', size = 10, sort = 'nombre,asc') {
    return this.api.get<Pagina<Empleado>>('/admin/empleados/pagina', {
      page,
      size,
      texto,
      sort,
    });
  }
  vincular(datos: DatosAltaLaboral & { usuarioId: number }) {
    return this.api.post<Empleado>('/admin/empleados', datos);
  }
  crearEmpleado(datos: DatosAltaLaboral & { usuario: CrearUsuario; telefono: string }) {
    return this.api.post<Empleado>('/admin/empleados/con-usuario', datos);
  }
  actualizarLaboral(id: number, datos: DatosLaborales) {
    return this.api.put<Empleado>(`/admin/empleados/${id}`, datos);
  }
  actualizarPerfil(id: number, datos: DatosPerfil) {
    return this.api.put<Empleado>(`/admin/empleados/${id}/perfil`, datos);
  }
  cambiarRol(id: number, rol: RolUsuario) {
    return this.api.patch<Empleado>(`/admin/empleados/${id}/rol`, { rol });
  }
  activarEmpleado(id: number, activo: boolean) {
    return this.api.patch<Empleado>(`/admin/empleados/${id}/activo`, { activo });
  }
  eliminarEmpleado(id: number) {
    return this.api.delete<void>(`/admin/empleados/${id}`);
  }
  empresa() {
    return this.api.get<Empresa>('/admin/empresa');
  }
  guardarEmpresa(datos: DatosEmpresa) {
    return this.api.put<Empresa>('/admin/empresa', datos);
  }
}
