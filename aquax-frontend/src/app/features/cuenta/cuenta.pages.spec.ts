import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { PreferencesService } from '../../core/preferences.service';
import { PerfilPage } from './perfil.page';
import { PreferenciasPage } from './preferencias.page';

const perfil = {
  id: 1,
  nombreUsuario: 'ana',
  nombre: 'Ana',
  apellido: 'Pérez',
  correo: 'ana@example.com',
  telefono: '999888777',
  rol: 'VENDEDOR',
  activo: true,
};
const preferencias = {
  usuarioId: 1,
  idioma: 'es',
  nivelZoom: 100,
  filtroColor: 'none',
  filtroColorActivo: false,
};

describe('Cuenta: confirmación de escrituras y recuperación de errores', () => {
  let http: HttpTestingController;
  const auth = { updateUser: vi.fn(), logout: vi.fn(), user: () => perfil, isAdmin: () => false };
  const visual = { apply: vi.fn(), reset: vi.fn() };
  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      imports: [PerfilPage, PreferenciasPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: PreferencesService, useValue: visual },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('conserva el formulario y la identidad si el servidor rechaza un correo duplicado', () => {
    const fixture = TestBed.createComponent(PerfilPage);
    fixture.detectChanges();
    http.expectOne('/api/personal/perfil').flush(perfil);
    const page = fixture.componentInstance;
    page.form.controls.correo.setValue('duplicado@example.com');
    page.save();
    page.save();
    http
      .expectOne('/api/personal/perfil')
      .flush({ detail: 'El correo ya está registrado.' }, { status: 409, statusText: 'Conflict' });
    expect(page.form.controls.correo.value).toBe('duplicado@example.com');
    expect(auth.updateUser).not.toHaveBeenCalled();
    expect(page.saved()).toBe(false);
    expect(page.busy()).toBe(false);
    expect(page.error()).toContain('El correo ya está registrado.');
    http.expectNone('/api/personal/perfil');
  });

  it('solo cierra sesión cuando el cambio de contraseña es confirmado por Spring', () => {
    const fixture = TestBed.createComponent(PerfilPage);
    fixture.detectChanges();
    http.expectOne('/api/personal/perfil').flush(perfil);
    const page = fixture.componentInstance;
    page.passwordForm.setValue({
      contrasenaActual: 'Actual2026!',
      nuevaContrasena: 'Nueva2026!',
      confirmarContrasena: 'Nueva2026!',
    });
    page.changePassword();
    page.changePassword();
    http
      .expectOne('/api/personal/perfil/contrasena')
      .flush({ detail: 'No se pudo completar.' }, { status: 500, statusText: 'Server Error' });
    expect(auth.logout).not.toHaveBeenCalled();
    expect(page.passwordBusy()).toBe(false);
    expect(page.passwordForm.controls.nuevaContrasena.value).toBe('Nueva2026!');
    page.changePassword();
    http.expectOne('/api/personal/perfil/contrasena').flush({ mensaje: 'Actualizada' });
    expect(auth.logout).toHaveBeenCalledOnce();
  });

  it('aplica preferencias visuales solamente después de una respuesta exitosa', () => {
    const fixture = TestBed.createComponent(PreferenciasPage);
    fixture.detectChanges();
    http.expectOne('/api/personal/preferencias').flush(preferencias);
    expect(visual.apply).toHaveBeenCalledWith(preferencias);
    visual.apply.mockClear();
    const page = fixture.componentInstance;
    page.form.controls.nivelZoom.setValue(120);
    page.save();
    http
      .expectOne('/api/personal/preferencias')
      .flush({ detail: 'Error de persistencia.' }, { status: 500, statusText: 'Server Error' });
    expect(visual.apply).not.toHaveBeenCalled();
    expect(page.busy()).toBe(false);
    expect(page.form.controls.nivelZoom.value).toBe(120);
    page.save();
    http.expectOne('/api/personal/preferencias').flush({ ...preferencias, nivelZoom: 120 });
    expect(visual.apply).toHaveBeenCalledWith({ ...preferencias, nivelZoom: 120 });
    expect(page.saved()).toBe(true);
  });
});
