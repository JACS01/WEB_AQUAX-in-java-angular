import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { Empleado, RolUsuario } from '../../core/models';
import { AdminService } from './admin.service';
import { EmpleadosPage } from './empleados.page';
import { EmpresaPage } from './empresa.page';
import { UsuariosPage } from './usuarios.page';

const account = {
  nombreUsuario: 'ana_1',
  nombre: 'Ana',
  apellido: 'Pérez',
  correo: 'ana@example.com',
  contrasena: 'Prueba2026!',
  rol: 'VENDEDOR' as RolUsuario,
};
const employee = {
  id: 12,
  usuarioId: 2,
  nombreUsuario: 'ana_1',
  nombre: 'Ana',
  apellido: 'Pérez',
  correo: 'ana@example.com',
  telefono: '+51 999888777',
  rol: 'VENDEDOR',
  activo: true,
  dni: '12345678',
  fechaIngreso: '2026-11-11',
} as Empleado;
const company = {
  razonSocial: 'AquaX',
  ruc: '12345678901',
  direccion: 'Av. Lima 123',
  correo: '',
  telefono1: '',
  telefono2: '',
  registroSanitario: '',
  slogan: '',
};
function email(length: number) {
  const labels: string[] = [];
  let remaining = length - 65;
  while (remaining > 63) {
    labels.push('b'.repeat(63));
    remaining -= 64;
  }
  labels.push('c'.repeat(remaining));
  return 'a'.repeat(64) + '@' + labels.join('.');
}
const invalidAccount: [string, unknown][] = [
  ['nombreUsuario', 'ab'],
  ['nombreUsuario', 'a'.repeat(51)],
  ['nombreUsuario', 'ana perez'],
  ['nombreUsuario', 'ana@1'],
  ['nombre', '   '],
  ['nombre', null],
  ['nombre', 'a'.repeat(101)],
  ['apellido', '\t'],
  ['apellido', null],
  ['apellido', 'a'.repeat(101)],
  ['correo', ''],
  ['correo', null],
  ['correo', 'correo-invalido'],
  ['correo', email(151)],
  ['contrasena', '1234567'],
  ['contrasena', null],
  ['contrasena', ' '.repeat(8)],
  ['contrasena', 'a'.repeat(73)],
  ['contrasena', 'á'.repeat(37)],
  ['rol', 'SUPERVISOR'],
  ['rol', ''],
];
const invalidLabor: [string, unknown][] = [
  ['dni', '1234567'],
  ['dni', '123456789'],
  ['dni', '1234567a'],
  ['fechaIngreso', '2026-02-30'],
  ['fechaIngreso', '2025-02-29'],
  ['fechaIngreso', '11/11/2026'],
];
const invalidProfile: [string, unknown][] = [
  ['nombre', ' '],
  ['nombre', 'a'.repeat(101)],
  ['apellido', ' '],
  ['apellido', 'a'.repeat(101)],
  ['correo', ''],
  ['correo', 'mal'],
  ['correo', email(151)],
  ['telefono', ' '],
  ['telefono', '1'.repeat(16)],
];

describe('Administración: límites de los DTO y bloqueo de escrituras inválidas', () => {
  let service: { [key: string]: ReturnType<typeof vi.fn> };
  beforeEach(() => {
    service = Object.fromEntries(
      [
        'crearUsuario',
        'activarUsuario',
        'vincular',
        'crearEmpleado',
        'actualizarLaboral',
        'actualizarPerfil',
        'cambiarRol',
        'activarEmpleado',
        'eliminarEmpleado',
        'guardarEmpresa',
      ].map((name) => [name, vi.fn(() => new Subject())]),
    );
    service['usuarios'] = vi.fn(() =>
      of({ content: [], page: { number: 0, size: 10, totalElements: 0, totalPages: 0 } }),
    );
    service['empleados'] = vi.fn(() =>
      of({ content: [], page: { number: 0, size: 10, totalElements: 0, totalPages: 0 } }),
    );
    service['cuentas'] = vi.fn(() => of([{ id: 2, ...account, activo: true }]));
    service['empresa'] = vi.fn(() => of({ ...company, id: 1, ultimaActualizacion: '2026-11-11' }));
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

  it.each(invalidAccount)('no crea usuarios con %s = %s', (field, value) => {
    const page = TestBed.createComponent(UsuariosPage).componentInstance;
    page.form.setValue(account);
    page.form.get(field)!.setValue(value);
    page.create();
    expect(page.form.get(field)!.invalid).toBe(true);
    expect(service['crearUsuario']).not.toHaveBeenCalled();
  });
  it('admite los límites válidos y bloquea el segundo envío mientras la creación está pendiente', () => {
    const page = TestBed.createComponent(UsuariosPage).componentInstance;
    page.form.setValue({
      ...account,
      nombreUsuario: 'a'.repeat(50),
      nombre: 'a'.repeat(100),
      apellido: 'a'.repeat(100),
      correo: email(150),
      contrasena: 'á'.repeat(36),
    });
    expect(page.form.valid).toBe(true);
    page.create();
    page.create();
    expect(service['crearUsuario']).toHaveBeenCalledOnce();
    page.form.controls.nombreUsuario.setValue('a.-');
    expect(page.form.controls.nombreUsuario.valid).toBe(true);
  });

  it.each([...invalidAccount, ...invalidLabor, ['telefono', ' '], ['telefono', '1'.repeat(16)]] as [
    string,
    unknown,
  ][])('no crea empleados con %s = %s', (field, value) => {
    const page = TestBed.createComponent(EmpleadosPage).componentInstance;
    page.createForm.setValue({
      ...account,
      telefono: '+51 999888777',
      dni: '12345678',
      fechaIngreso: '2026-11-11',
    });
    page.createForm.get(field)!.setValue(value);
    page.create();
    expect(page.createForm.get(field)!.invalid).toBe(true);
    expect(service['crearEmpleado']).not.toHaveBeenCalled();
  });
  it('permite omitir la fecha al crear y vincular; Spring asigna la fecha predeterminada', () => {
    const page = TestBed.createComponent(EmpleadosPage).componentInstance;
    page.createForm.setValue({
      ...account,
      telefono: '+51 999888777',
      dni: '12345678',
      fechaIngreso: '',
    });
    page.create();
    expect(service['crearEmpleado']).toHaveBeenCalledWith({
      usuario: account,
      telefono: '+51 999888777',
      dni: '12345678',
    });
    page.saving.set(false);
    page.open('link');
    page.linkForm.setValue({ usuarioId: 2, dni: '87654321', fechaIngreso: '' });
    page.link();
    expect(service['vincular']).toHaveBeenCalledWith({ usuarioId: 2, dni: '87654321' });
  });
  it.each([0, -1, 1.5, Number.MAX_SAFE_INTEGER + 1, '1e3', '2', ''])(
    'rechaza el identificador de cuenta inválido %s',
    (id) => {
      const page = TestBed.createComponent(EmpleadosPage).componentInstance;
      page.open('link');
      page.linkForm.patchValue({ dni: '12345678' });
      page.linkForm.controls.usuarioId.setValue(id as number);
      page.link();
      expect(service['vincular']).not.toHaveBeenCalled();
    },
  );
  it('no vincula antes de cargar las cuentas ni cuando esa consulta falla', () => {
    const result = new Subject();
    service['cuentas'].mockReturnValue(result);
    const page = TestBed.createComponent(EmpleadosPage).componentInstance;
    page.open('link');
    page.linkForm.setValue({ usuarioId: 2, dni: '12345678', fechaIngreso: '' });
    page.link();
    expect(service['vincular']).not.toHaveBeenCalled();
    result.error(new Error('Conexión'));
    page.link();
    expect(page.accountsLoading()).toBe(false);
    expect(page.accountsLoaded()).toBe(false);
    expect(service['vincular']).not.toHaveBeenCalled();
  });

  it.each([...invalidLabor, ['fechaIngreso', '']] as [string, unknown][])(
    'no actualiza datos laborales con %s = %s',
    (field, value) => {
      const page = TestBed.createComponent(EmpleadosPage).componentInstance;
      page.edit(employee);
      page.laborForm.get(field)!.setValue(value);
      page.saveLabor();
      expect(page.laborForm.get(field)!.invalid).toBe(true);
      expect(service['actualizarLaboral']).not.toHaveBeenCalled();
    },
  );
  it('acepta un día bisiesto real al editar y bloquea el envío laboral repetido', () => {
    const page = TestBed.createComponent(EmpleadosPage).componentInstance;
    page.edit(employee);
    page.laborForm.controls.fechaIngreso.setValue('2024-02-29');
    page.saveLabor();
    page.saveLabor();
    expect(service['actualizarLaboral']).toHaveBeenCalledOnce();
  });
  it.each(invalidProfile)('no actualiza el perfil de empleado con %s = %s', (field, value) => {
    const page = TestBed.createComponent(EmpleadosPage).componentInstance;
    page.edit(employee);
    page.profileForm.get(field)!.setValue(value);
    page.saveProfile();
    expect(service['actualizarPerfil']).not.toHaveBeenCalled();
  });
  it('no modifica fichas sin selección y rechaza un rol fuera del contrato', () => {
    const page = TestBed.createComponent(EmpleadosPage).componentInstance;
    page.laborForm.setValue({ dni: '12345678', fechaIngreso: '2026-11-11' });
    page.profileForm.setValue({
      nombre: 'Ana',
      apellido: 'Pérez',
      correo: 'ana@example.com',
      telefono: '123',
    });
    page.saveLabor();
    page.saveProfile();
    page.saveRole();
    expect(service['actualizarLaboral']).not.toHaveBeenCalled();
    expect(service['actualizarPerfil']).not.toHaveBeenCalled();
    expect(service['cambiarRol']).not.toHaveBeenCalled();
    page.edit(employee);
    page.role.setValue('SUPERVISOR' as RolUsuario);
    page.saveRole();
    expect(page.role.invalid).toBe(true);
    expect(service['cambiarRol']).not.toHaveBeenCalled();
  });

  it.each([
    ['razonSocial', ' '],
    ['razonSocial', 'a'.repeat(151)],
    ['ruc', '1234567890'],
    ['ruc', '123456789012'],
    ['ruc', '1234567890a'],
    ['direccion', ' '],
    ['direccion', 'a'.repeat(301)],
    ['telefono1', '1'.repeat(16)],
    ['telefono2', '1'.repeat(16)],
    ['correo', 'mal'],
    ['correo', email(201)],
    ['registroSanitario', 'a'.repeat(501)],
    ['slogan', 'a'.repeat(201)],
  ] as [string, unknown][])('no guarda empresa con %s = %s', (field, value) => {
    const page = TestBed.createComponent(EmpresaPage).componentInstance;
    page.form.get(field)!.setValue(value);
    page.save();
    expect(page.form.get(field)!.invalid).toBe(true);
    expect(service['guardarEmpresa']).not.toHaveBeenCalled();
  });
  it('acepta los límites empresariales y los campos opcionales vacíos sin exigir teléfono numérico', () => {
    const page = TestBed.createComponent(EmpresaPage).componentInstance;
    expect(page.form.valid).toBe(true);
    page.form.patchValue({
      razonSocial: 'a'.repeat(150),
      direccion: 'a'.repeat(300),
      telefono1: '+51 999888777',
      telefono2: '1'.repeat(15),
      correo: email(200),
      registroSanitario: 'a'.repeat(500),
      slogan: 'a'.repeat(200),
    });
    expect(page.form.valid).toBe(true);
    page.save();
    page.save();
    expect(service['guardarEmpresa']).toHaveBeenCalledOnce();
  });
  it('no guarda datos empresariales antes de completar una carga exitosa', () => {
    const result = new Subject();
    service['empresa'].mockReturnValue(result);
    const page = TestBed.createComponent(EmpresaPage).componentInstance;
    page.form.setValue(company);
    page.save();
    expect(service['guardarEmpresa']).not.toHaveBeenCalled();
    result.error(new Error('Sin conexión'));
    page.save();
    expect(page.loaded()).toBe(false);
    expect(service['guardarEmpresa']).not.toHaveBeenCalled();
  });
});
