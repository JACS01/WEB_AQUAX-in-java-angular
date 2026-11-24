import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, Subject } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth/auth.service';
import { Empleado, Pagina, Usuario } from '../../core/models';
import { AdminService } from './admin.service';
import { UsuariosPage } from './usuarios.page';
import { EmpleadosPage } from './empleados.page';

const usuario: Usuario = {
  id: 7,
  nombreUsuario: 'ana',
  nombre: 'Ana',
  apellido: 'Pérez',
  correo: 'ana@example.com',
  rol: 'VENDEDOR',
  activo: true,
};
const empleado: Empleado = {
  ...usuario,
  usuarioId: 7,
  dni: '12345678',
  fechaIngreso: '2026-11-11',
  telefono: '999888777',
};
function page<T>(content: T[], number = 0, size = 10, totalPages = 5): Pagina<T> {
  return { content, page: { number, size, totalElements: totalPages * size, totalPages } };
}

describe('Tablas administrativas: URL y consultas reactivas', () => {
  const service = { usuarios: vi.fn(), empleados: vi.fn() };
  beforeEach(() => {
    vi.resetAllMocks();
    service.usuarios.mockImplementation((number: number, _texto: string, size: number) =>
      of(page([usuario], number, size)),
    );
    service.empleados.mockImplementation((number: number, _texto: string, size: number) =>
      of(page([empleado], number, size)),
    );
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'usuarios', component: UsuariosPage },
          { path: 'empleados', component: EmpleadosPage },
        ]),
        { provide: AdminService, useValue: service },
        {
          provide: AuthService,
          useValue: { user: signal(null), updateUser: vi.fn(), logout: vi.fn() },
        },
      ],
    });
  });
  afterEach(() => vi.useRealTimers());

  it('restaura filtros, página, tamaño y orden de usuarios y atiende cambios de URL', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl(
      '/usuarios?page=2&size=25&texto=P%C3%A9rez&sort=correo,desc',
      UsuariosPage,
    );
    expect(service.usuarios).toHaveBeenLastCalledWith(2, 'Pérez', 25, 'correo,desc');
    expect(component.search.value).toBe('Pérez');
    expect(component.table.page()).toBe(2);
    await harness.navigateByUrl('/usuarios?page=1&size=10&texto=Ana&sort=nombre,asc', UsuariosPage);
    expect(service.usuarios).toHaveBeenLastCalledWith(1, 'Ana', 10, 'nombre,asc');
    expect(component.search.value).toBe('Ana');
  });

  it('espera 300 ms al escribir y vuelve a la primera página sin perder el tamaño', async () => {
    // Control the initial URL feedback timer as well as subsequent typing intervals.
    // Angular's rendering timers remain real.
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval', 'Date'] });
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/usuarios?page=2&size=25', UsuariosPage);
    service.usuarios.mockClear();
    component.search.setValue('A');
    await vi.advanceTimersByTimeAsync(180);
    component.search.setValue('Ana');
    await vi.advanceTimersByTimeAsync(299);
    expect(service.usuarios).not.toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(1);
    await harness.fixture.whenStable();
    expect(service.usuarios).toHaveBeenCalledOnce();
    expect(service.usuarios).toHaveBeenLastCalledWith(0, 'Ana', 25, 'nombre,asc');
    expect(TestBed.inject(Router).url).toContain('texto=Ana');
    expect(TestBed.inject(Router).url).toContain('page=0');
  });

  it('cancela la consulta anterior de empleados y permite reintentar tras un error', async () => {
    const old = new Subject<Pagina<Empleado>>();
    const current = new Subject<Pagina<Empleado>>();
    service.empleados
      .mockReturnValueOnce(old)
      .mockReturnValueOnce(current)
      .mockReturnValue(of(page([empleado])));
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/empleados?texto=Anterior', EmpleadosPage);
    expect(old.observed).toBe(true);
    await harness.navigateByUrl('/empleados?texto=Actual', EmpleadosPage);
    expect(old.observed).toBe(false);
    current.error(
      new HttpErrorResponse({
        status: 503,
        error: { detail: 'Servidor temporalmente no disponible.' },
      }),
    );
    expect(component.loading()).toBe(false);
    expect(component.error()).toContain('Servidor temporalmente no disponible.');
    component.load();
    expect(component.pagina()?.content[0].id).toBe(empleado.id);
    expect(component.error()).toBe('');
    expect(service.empleados).toHaveBeenLastCalledWith(0, 'Actual', 10, 'nombre,asc');
  });

  it('corrige una página fuera de rango directamente a la última disponible', async () => {
    service.empleados.mockImplementation((number: number) =>
      of(page(number === 99 ? [] : [empleado], number, 10, 3)),
    );
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empleados?page=99');
    await harness.fixture.whenStable();
    expect(service.empleados.mock.calls.map((call) => call[0])).toEqual([99, 2]);
    expect(TestBed.inject(Router).url).toContain('page=2');
  });
});
