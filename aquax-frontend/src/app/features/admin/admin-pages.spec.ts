import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth/auth.service';
import { Empleado, Usuario } from '../../core/models';
import { AdminService } from './admin.service';
import { EmpleadosPage } from './empleados.page';
import { UsuariosPage } from './usuarios.page';

const admin: Usuario = {
  id: 1,
  nombreUsuario: 'ana',
  nombre: 'Ana',
  apellido: 'Pérez',
  correo: 'ana@example.com',
  rol: 'ADMINISTRADOR',
  activo: true,
};
const employee: Empleado = {
  ...admin,
  id: 12,
  usuarioId: 1,
  dni: '12345678',
  fechaIngreso: '2026-11-11',
  telefono: '999888777',
};
const metadata = { size: 10, number: 0, totalElements: 1, totalPages: 1 };

describe('Administración: respuestas de negocio y acceso propio', () => {
  const service = {
    usuarios: vi.fn(),
    empleados: vi.fn(),
    activarUsuario: vi.fn(),
    cambiarRol: vi.fn(),
    crearUsuario: vi.fn(),
  };
  const auth = { user: signal<Usuario | null>(admin), updateUser: vi.fn(), logout: vi.fn() };
  const router = { navigateByUrl: vi.fn(), navigate: vi.fn() };

  beforeEach(() => {
    vi.resetAllMocks();
    auth.user.set(admin);
    service.usuarios.mockReturnValue(of({ content: [admin], page: metadata }));
    service.empleados.mockReturnValue(of({ content: [employee], page: metadata }));
    TestBed.configureTestingModule({
      providers: [
        { provide: AdminService, useValue: service },
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap({}) },
            queryParamMap: of(convertToParamMap({})),
          },
        },
      ],
    });
  });

  it('conserva la cuenta activa y muestra el conflicto al intentar desactivar al último administrador', () => {
    service.activarUsuario.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { detail: 'Debe permanecer al menos un administrador activo.' },
          }),
      ),
    );
    const page = TestBed.createComponent(UsuariosPage).componentInstance;
    page.toggle(admin);
    expect(page.pagina()?.content[0].activo).toBe(true);
    expect(page.error()).toContain('Debe permanecer al menos un administrador activo.');
    expect(page.saving()).toBe(false);
    expect(page.success()).toBe('');
    expect(auth.logout).not.toHaveBeenCalled();
  });

  it('muestra los errores por campo al enviar desde el DOM una cuenta vacía', async () => {
    const fixture = TestBed.createComponent(UsuariosPage);
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;
    element.querySelector<HTMLButtonElement>('header button')!.click();
    await fixture.whenStable();
    expect(element.querySelectorAll('app-admin-field-error .field-error')).toHaveLength(0);
    element
      .querySelector<HTMLFormElement>('section form')!
      .dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    await fixture.whenStable();
    expect(element.querySelectorAll('app-admin-field-error .field-error')).toHaveLength(5);
    expect(element.textContent).toContain('Este campo es obligatorio.');
    expect(service.crearUsuario).not.toHaveBeenCalled();
  });

  it('cierra la sesión después de desactivar la cuenta propia con éxito', () => {
    service.activarUsuario.mockReturnValue(of({ ...admin, activo: false }));
    const page = TestBed.createComponent(UsuariosPage).componentInstance;
    page.toggle(admin);
    expect(auth.logout).toHaveBeenCalledOnce();
  });

  it('retira al usuario del área administrativa cuando cambia su propio rol a vendedor', () => {
    service.cambiarRol.mockReturnValue(of({ ...employee, rol: 'VENDEDOR' }));
    const page = TestBed.createComponent(EmpleadosPage).componentInstance;
    page.edit(employee);
    page.role.setValue('VENDEDOR');
    page.saveRole();
    expect(auth.updateUser).toHaveBeenCalledWith(expect.objectContaining({ rol: 'VENDEDOR' }));
    expect(router.navigateByUrl).toHaveBeenCalledWith('/panel');
    expect(page.saving()).toBe(false);
  });
});
