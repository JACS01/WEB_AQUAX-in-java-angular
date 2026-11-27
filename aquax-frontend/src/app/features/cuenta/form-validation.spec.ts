import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { PreferencesService } from '../../core/preferences.service';
import { LoginPage } from '../auth/login.page';
import { PedidoForm } from '../catalogo/pedido-form';
import { PerfilPage } from './perfil.page';
import { PreferenciasPage } from './preferencias.page';

const person = {
  nombre: 'Ana',
  apellido: 'Pérez',
  correo: 'ana@example.com',
  telefono: '999888777',
};
const profile = { ...person, id: 1, nombreUsuario: 'ana', rol: 'VENDEDOR', activo: true };
const preference = {
  usuarioId: 1,
  idioma: 'es',
  nivelZoom: 100,
  filtroColor: 'none',
  filtroColorActivo: false,
};
const customer = {
  nombreCliente: 'Ana',
  telefonoCliente: '+51 999 888 777',
  direccionEntrega: 'Av. Lima 123',
  observaciones: '',
};
const products = [{ id: 1, nombre: 'Agua', descripcion: 'Bidón', precio: 10 }];
const validPassword = {
  contrasenaActual: 'Actual2026!',
  nuevaContrasena: 'Nueva2026!',
  confirmarContrasena: 'Nueva2026!',
};

describe('Contratos de validación de acceso, catálogo y cuenta', () => {
  let http: HttpTestingController;
  const auth = {
    login: vi.fn(),
    logout: vi.fn(),
    updateUser: vi.fn(),
    user: () => profile,
    isAdmin: () => false,
  };
  const visual = { apply: vi.fn(), reset: vi.fn() };
  beforeEach(() => {
    vi.clearAllMocks();
    auth.login.mockImplementation(() => new Subject());
    TestBed.configureTestingModule({
      imports: [LoginPage, PedidoForm, PerfilPage, PreferenciasPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: PreferencesService, useValue: visual },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  function perfil() {
    const fixture = TestBed.createComponent(PerfilPage);
    fixture.detectChanges();
    http.expectOne('/api/personal/perfil').flush(profile);
    return fixture.componentInstance;
  }
  function preferencias() {
    const fixture = TestBed.createComponent(PreferenciasPage);
    fixture.detectChanges();
    http.expectOne('/api/personal/preferencias').flush(preference);
    return fixture.componentInstance;
  }
  function pedido() {
    const fixture = TestBed.createComponent(PedidoForm);
    fixture.detectChanges();
    http.expectOne('/api/productos').flush(products);
    const page = fixture.componentInstance;
    page.form.setValue(customer);
    page.setQuantity(1, 1);
    return page;
  }

  it.each([
    ['nombreUsuario', ''],
    ['nombreUsuario', '   '],
    ['nombreUsuario', 'u'.repeat(51)],
    ['contrasena', ''],
    ['contrasena', '        '],
    ['contrasena', 'p'.repeat(73)],
  ])('login rechaza %s inválido sin enviar credenciales (%s)', (field, value) => {
    const page = TestBed.createComponent(LoginPage).componentInstance;
    page.form.setValue({ nombreUsuario: 'ana', contrasena: 'Actual2026!' });
    page.form.get(field)!.setValue(value);
    page.submit();
    expect(auth.login).not.toHaveBeenCalled();
    expect(page.form.get(field)!.touched).toBe(true);
    expect(page.busy()).toBe(false);
  });
  it('login acepta los límites y bloquea dos envíos mientras espera al servidor', () => {
    const page = TestBed.createComponent(LoginPage).componentInstance;
    page.form.setValue({ nombreUsuario: 'u'.repeat(50), contrasena: 'p'.repeat(72) });
    page.submit();
    page.submit();
    expect(auth.login).toHaveBeenCalledOnce();
  });

  it.each([
    ['nombreCliente', ''],
    ['nombreCliente', '  '],
    ['nombreCliente', 'a'.repeat(151)],
    ['telefonoCliente', ''],
    ['telefonoCliente', '  '],
    ['telefonoCliente', '1'.repeat(16)],
    ['direccionEntrega', ''],
    ['direccionEntrega', '  '],
    ['direccionEntrega', 'a'.repeat(301)],
    ['observaciones', 'a'.repeat(501)],
  ])('pedido rechaza %s fuera del contrato sin POST (%s)', (field, value) => {
    const page = pedido();
    page.form.get(field)!.setValue(value);
    page.reviewOrder();
    http.expectNone((r) => r.method === 'POST');
    expect(page.form.get(field)!.touched).toBe(true);
  });
  it('pedido acepta todas las longitudes máximas y no impone teléfono exclusivamente numérico', () => {
    const page = pedido();
    page.form.setValue({
      nombreCliente: 'a'.repeat(150),
      telefonoCliente: '+51 999 888 777',
      direccionEntrega: 'a'.repeat(300),
      observaciones: 'a'.repeat(500),
    });
    page.reviewOrder();
    page.submit();
    http.expectOne('/api/pedidos').flush({ id: 1 });
  });
  it.each([0, -1, 1.5, 10000, Number.NaN, Number.POSITIVE_INFINITY])(
    'pedido no permite una cantidad inválida: %s',
    (value) => {
      const page = pedido();
      page.setQuantity(1, 0);
      page.setQuantity(1, value);
      page.reviewOrder();
      expect(page.reviewing()).toBe(false);
      http.expectNone((r) => r.method === 'POST');
      expect(page.units()).toBe(0);
    },
  );
  it('pedido no envía una selección ajena al catálogo y no puede crearse durante una recarga', () => {
    const page = pedido();
    page.setQuantity(1, 0);
    page.setQuantity(999, 2);
    page.reviewOrder();
    expect(page.reviewing()).toBe(false);
    http.expectNone((r) => r.method === 'POST');
    page.setQuantity(1, 1);
    page.reviewOrder();
    page.load();
    page.submit();
    http.expectNone((r) => r.method === 'POST');
    http.expectOne('/api/productos').flush(products);
  });
  it('pedido bloquea el envío si la última carga del catálogo falló', () => {
    const page = pedido();
    page.reviewOrder();
    page.load();
    http
      .expectOne('/api/productos')
      .flush({ detail: 'Catálogo no disponible' }, { status: 503, statusText: 'Unavailable' });
    page.submit();
    http.expectNone((r) => r.method === 'POST');
  });

  it.each([
    ['nombre', ''],
    ['nombre', '  '],
    ['nombre', 'a'.repeat(101)],
    ['apellido', ''],
    ['apellido', '  '],
    ['apellido', 'a'.repeat(101)],
    ['correo', ''],
    ['correo', '  '],
    ['correo', 'correo-invalido'],
    ['correo', 'a'.repeat(139) + '@example.com'],
    ['telefono', ''],
    ['telefono', '  '],
    ['telefono', '1'.repeat(16)],
  ])('perfil rechaza %s inválido sin PUT (%s)', (field, value) => {
    const page = perfil();
    page.form.get(field)!.setValue(value);
    page.save();
    http.expectNone((r) => r.method === 'PUT');
    expect(page.form.get(field)!.touched).toBe(true);
  });
  it('perfil acepta límites máximos del DTO', () => {
    const page = perfil();
    page.form.setValue({
      nombre: 'a'.repeat(100),
      apellido: 'a'.repeat(100),
      correo: 'a'.repeat(64) + '@' + 'b'.repeat(60) + '.' + 'c'.repeat(24),
      telefono: '1'.repeat(15),
    });
    expect(page.form.controls.correo.value.length).toBe(150);
    page.save();
    http.expectOne('/api/personal/perfil').flush(profile);
  });
  it('perfil bloquea guardar antes de cargar o después de un error de carga', () => {
    const fixture = TestBed.createComponent(PerfilPage);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    page.form.setValue(person);
    page.save();
    http.expectNone((r) => r.method === 'PUT');
    http
      .expectOne('/api/personal/perfil')
      .flush({ detail: 'No disponible' }, { status: 503, statusText: 'Unavailable' });
    page.save();
    http.expectNone((r) => r.method === 'PUT');
  });
  it.each([
    ['contrasenaActual', ''],
    ['contrasenaActual', '        '],
    ['contrasenaActual', 'a'.repeat(73)],
    ['contrasenaActual', 'é'.repeat(37)],
    ['nuevaContrasena', ''],
    ['nuevaContrasena', '        '],
    ['nuevaContrasena', '1234567'],
    ['nuevaContrasena', 'a'.repeat(73)],
    ['nuevaContrasena', 'é'.repeat(37)],
    ['confirmarContrasena', ''],
    ['confirmarContrasena', '        '],
    ['confirmarContrasena', 'a'.repeat(73)],
  ])('contraseña rechaza %s inválido sin PATCH (%s)', (field, value) => {
    const page = perfil();
    page.passwordForm.setValue(validPassword);
    page.passwordForm.get(field)!.setValue(value);
    page.changePassword();
    http.expectNone('/api/personal/perfil/contrasena');
    expect(page.passwordForm.get(field)!.touched).toBe(true);
  });
  it('contraseña exige confirmación coincidente', () => {
    const page = perfil();
    page.passwordForm.setValue({ ...validPassword, confirmarContrasena: 'Otra2026!' });
    page.changePassword();
    http.expectNone('/api/personal/perfil/contrasena');
    expect(page.passwordError()).toContain('no coinciden');
  });
  it.each(['12345678', 'a'.repeat(72), 'é'.repeat(36)])(
    'contraseña acepta límites en caracteres y UTF-8 (%s)',
    (value) => {
      const page = perfil();
      page.passwordForm.setValue({
        ...validPassword,
        nuevaContrasena: value,
        confirmarContrasena: value,
      });
      page.changePassword();
      http.expectOne('/api/personal/perfil/contrasena').flush({ mensaje: 'Actualizada' });
      expect(auth.logout).toHaveBeenCalledOnce();
    },
  );

  it.each([
    ['idioma', ''],
    ['idioma', 'fr'],
    ['nivelZoom', 0],
    ['nivelZoom', 100.5],
    ['nivelZoom', 140],
    ['nivelZoom', '100'],
    ['filtroColor', ''],
    ['filtroColor', 'desconocido'],
    ['filtroColorActivo', null],
    ['filtroColorActivo', 'false'],
  ])('preferencias rechazan %s fuera del catálogo permitido (%s)', (field, value) => {
    const page = preferencias();
    page.form.get(field)!.setValue(value);
    page.save();
    http.expectNone((r) => r.method === 'PUT');
    expect(page.error()).toContain('Revisa');
  });
  it.each([80, 90, 100, 110, 120, 130, 150])('preferencias aceptan zoom permitido %s', (value) => {
    const page = preferencias();
    page.form.controls.nivelZoom.setValue(value);
    page.save();
    http.expectOne('/api/personal/preferencias').flush({ ...preference, nivelZoom: value });
  });
  it('preferencias bloquean escrituras durante una recarga o si la recarga falló', () => {
    const page = preferencias();
    page.load();
    page.save();
    http.expectNone((r) => r.method === 'PUT');
    http
      .expectOne('/api/personal/preferencias')
      .flush({ detail: 'No disponible' }, { status: 503, statusText: 'Unavailable' });
    page.save();
    http.expectNone((r) => r.method === 'PUT');
  });
});
