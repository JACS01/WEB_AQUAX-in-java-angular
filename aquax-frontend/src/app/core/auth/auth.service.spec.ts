import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { Usuario } from '../models';
import { AuthService } from './auth.service';

const user: Usuario = {
  id: 1,
  nombreUsuario: 'ana',
  nombre: 'Ana',
  apellido: 'Pérez',
  correo: 'ana@example.com',
  rol: 'ADMINISTRADOR',
  activo: true,
};
const base = environment.apiUrl;

describe('AuthService', () => {
  let http: HttpTestingController;
  const navigate = vi.fn().mockResolvedValue(true);

  beforeEach(() => {
    sessionStorage.clear();
    navigate.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: { url: '/panel/pedidos', navigate } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  function login(auth: AuthService): void {
    auth.login({ nombreUsuario: 'ana', contrasena: 'Privada2026!' }).subscribe();
    const request = http.expectOne(`${base}/auth/login`);
    expect(request.request.method).toBe('POST');
    request.flush({ token: 'signed-token', tokenType: 'Bearer', expiresIn: 3600, usuario: user });
  }

  it('saves only the token and expiry, never credentials or a trusted persisted role', () => {
    const auth = TestBed.inject(AuthService);
    login(auth);
    expect(auth.token).toBe('signed-token');
    expect(auth.isAdmin()).toBe(true);
    const saved = sessionStorage.getItem('aquax.session')!;
    expect(saved).not.toContain('Privada2026!');
    expect(saved).not.toContain('ADMINISTRADOR');
    expect(JSON.parse(saved).expiresAt).toBeGreaterThan(Date.now());
  });

  it('restores a token and loads the current role from the backend', () => {
    sessionStorage.setItem(
      'aquax.session',
      JSON.stringify({
        token: 'restored',
        expiresAt: Date.now() + 10000,
        user: { ...user, rol: 'ADMINISTRADOR' },
      }),
    );
    const auth = TestBed.inject(AuthService);
    expect(auth.user()).toBeNull();
    expect(auth.isAdmin()).toBe(false);
    let valid = false;
    auth.ensureSession().subscribe((result) => {
      valid = result;
    });
    http.expectOne(`${base}/auth/me`).flush({ ...user, rol: 'VENDEDOR' });
    expect(valid).toBe(true);
    expect(auth.user()?.rol).toBe('VENDEDOR');
    expect(auth.isAdmin()).toBe(false);
  });

  it('rejects expired or malformed stored sessions without an HTTP call', () => {
    sessionStorage.setItem(
      'aquax.session',
      JSON.stringify({ token: 'expired', expiresAt: Date.now() - 1 }),
    );
    const auth = TestBed.inject(AuthService);
    let valid: boolean | undefined;
    auth.ensureSession().subscribe((result) => {
      valid = result;
    });
    expect(valid).toBe(false);
    expect(auth.token).toBeNull();
    http.expectNone(`${base}/auth/me`);
  });

  it('shares simultaneous validations and checks again on a later navigation', () => {
    const auth = TestBed.inject(AuthService);
    login(auth);
    const results: boolean[] = [];
    auth.ensureSession().subscribe((value) => results.push(value));
    auth.ensureSession().subscribe((value) => results.push(value));
    http.expectOne(`${base}/auth/me`).flush(user);
    expect(results).toEqual([true, true]);
    auth.ensureSession().subscribe();
    http.expectOne(`${base}/auth/me`).flush({ ...user, rol: 'VENDEDOR' });
    expect(auth.isAdmin()).toBe(false);
  });

  it('clears rejected credentials but retains a session when connectivity fails', () => {
    const auth = TestBed.inject(AuthService);
    login(auth);
    auth.ensureSession().subscribe((valid) => expect(valid).toBe(false));
    http.expectOne(`${base}/auth/me`).error(new ProgressEvent('network'));
    expect(auth.token).toBe('signed-token');
    auth.ensureSession().subscribe((valid) => expect(valid).toBe(false));
    http.expectOne(`${base}/auth/me`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.token).toBeNull();
    expect(auth.user()).toBeNull();
  });

  it('does not restore a user from an in-flight response after logout', () => {
    const auth = TestBed.inject(AuthService);
    login(auth);
    auth.ensureSession().subscribe((valid) => expect(valid).toBe(false));
    const request = http.expectOne(`${base}/auth/me`);
    auth.logout(false);
    request.flush(user);
    expect(auth.user()).toBeNull();
    expect(auth.isAuthenticated()).toBe(false);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('rejects an account deactivated since the token was issued', () => {
    const auth = TestBed.inject(AuthService);
    login(auth);
    auth.ensureSession().subscribe((valid) => expect(valid).toBe(false));
    http.expectOne(`${base}/auth/me`).flush({ ...user, activo: false });
    expect(auth.user()).toBeNull();
    expect(auth.token).toBeNull();
    expect(auth.isAdmin()).toBe(false);
  });

  it.each(['broken-json', 'null', '{}', '{"token":4,"expiresAt":99999999999999}'])(
    'rejects malformed storage %s without authenticating',
    (stored) => {
      sessionStorage.setItem('aquax.session', stored);
      const auth = TestBed.inject(AuthService);
      auth.ensureSession().subscribe((valid) => expect(valid).toBe(false));
      expect(auth.token).toBeNull();
      expect(auth.user()).toBeNull();
      http.expectNone(`${base}/auth/me`);
    },
  );
});
