import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es-PE';
import localePt from '@angular/common/locales/pt';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { TranslationService } from '../../core/i18n/translation.service';
import { Empleado, Usuario } from '../../core/models';
import { AdminService } from './admin.service';
import { EmpleadosPage } from './empleados.page';
import { UsuariosPage } from './usuarios.page';

registerLocaleData(localeEs);
registerLocaleData(localePt);

const user: Usuario = {
  id: 7,
  nombreUsuario: 'ana.perez',
  nombre: 'Ana',
  apellido: 'Pérez',
  correo: 'ana@example.com',
  rol: 'VENDEDOR',
  activo: true,
};
const employee: Empleado = {
  ...user,
  usuarioId: 7,
  dni: '12345678',
  fechaIngreso: '2026-11-11',
  telefono: '999888777',
};
const metadata = { number: 0, size: 10, totalElements: 1, totalPages: 1 };

describe('Administración en diferentes idiomas', () => {
  const service = { usuarios: vi.fn(), empleados: vi.fn(), crearUsuario: vi.fn() };
  beforeEach(() => {
    vi.clearAllMocks();
    service.usuarios.mockReturnValue(of({ content: [user], page: metadata }));
    service.empleados.mockReturnValue(of({ content: [employee], page: metadata }));
    service.crearUsuario.mockReturnValue(of(user));
    TestBed.configureTestingModule({
      providers: [
        { provide: AdminService, useValue: service },
        {
          provide: AuthService,
          useValue: { user: signal(null), updateUser: vi.fn(), logout: vi.fn() },
        },
        { provide: Router, useValue: { navigate: vi.fn(), navigateByUrl: vi.fn() } },
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

  it('traduce usuarios y el ordenamiento sin cambiar consultas ni datos de la cuenta', () => {
    const translations = TestBed.inject(TranslationService);
    translations.use('en');
    const fixture = TestBed.createComponent(UsuariosPage);
    fixture.detectChanges();
    const dom: HTMLElement = fixture.nativeElement;
    expect(dom.getAttribute('lang')).toBe('en');
    expect(dom.querySelector('h1')?.textContent).toBe('Users');
    expect(dom.textContent).toContain('Salesperson');
    expect(dom.textContent).toContain('Ana Pérez');
    expect(dom.textContent).toContain('ana@example.com');
    expect(service.usuarios).toHaveBeenLastCalledWith(0, '', 10, 'nombre,asc');
    const option = dom.querySelector<HTMLOptionElement>('option[value="correo,desc"]')!;
    expect(option.textContent).toContain('Email · descending');
    translations.use('pt');
    fixture.detectChanges();
    expect(dom.querySelector('h1')?.textContent).toBe('Usuários');
    expect(dom.querySelector('option[value="correo,desc"]')?.textContent).toContain(
      'E-mail · decrescente',
    );
    expect(service.usuarios).toHaveBeenCalledOnce();
  });

  it('traduce el formulario y la confirmación sin traducir el rol enviado a la API', () => {
    TestBed.inject(TranslationService).use('en');
    const fixture = TestBed.createComponent(UsuariosPage);
    const page = fixture.componentInstance;
    page.editing.set(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Create account');
    page.form.setValue({
      nombreUsuario: user.nombreUsuario,
      nombre: user.nombre,
      apellido: user.apellido,
      correo: user.correo,
      contrasena: 'Prueba2026!',
      rol: 'VENDEDOR',
    });
    page.create();
    expect(service.crearUsuario).toHaveBeenCalledWith(
      expect.objectContaining({ rol: 'VENDEDOR', nombre: 'Ana' }),
    );
    expect(page.success()).toBe('Account created successfully.');
  });

  it('traduce la confirmación de eliminar ficha y conserva nombre, usuario y DNI', () => {
    TestBed.inject(TranslationService).use('pt');
    const fixture = TestBed.createComponent(EmpleadosPage);
    fixture.componentInstance.deleting.set(employee);
    fixture.detectChanges();
    const dom: HTMLElement = fixture.nativeElement;
    expect(dom.querySelector('h1')?.textContent).toBe('Funcionários');
    expect(dom.querySelector('[role="alertdialog"] h2')?.textContent).toContain(
      'Excluir ficha de Ana Pérez',
    );
    expect(dom.querySelector('[role="alertdialog"] p')?.textContent).toContain('ana.perez');
    expect(dom.textContent).toContain('12345678');
    expect(dom.querySelector('button[aria-label="Editar Ana"]')).not.toBeNull();
  });
});
