import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { apiErrorInterceptor } from './api-error.interceptor';
import { authInterceptor } from './auth.interceptor';
import { errorMessage } from './api-error';

describe('API interceptors', () => {
  let http: HttpTestingController;
  let client: HttpClient;
  let auth: AuthService;
  const base = environment.apiUrl;
  const navigate = vi.fn().mockResolvedValue(true);

  beforeEach(() => {
    sessionStorage.clear();
    navigate.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor, apiErrorInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: { url: '/panel/pedidos', navigate } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
    auth = TestBed.inject(AuthService);
    auth.login({ nombreUsuario: 'ana', contrasena: 'test' }).subscribe();
    const login = http.expectOne(`${base}/auth/login`);
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush({
      token: 'private-token',
      expiresIn: 3600,
      tokenType: 'Bearer',
      usuario: {
        id: 1,
        nombreUsuario: 'ana',
        nombre: 'Ana',
        apellido: 'Pérez',
        correo: 'ana@example.com',
        rol: 'VENDEDOR',
        activo: true,
      },
    });
  });
  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('attaches Bearer only to protected AquaX requests', () => {
    client.get(`${base}/personal/pedidos`).subscribe();
    const protectedRequest = http.expectOne(`${base}/personal/pedidos`);
    expect(protectedRequest.request.headers.get('Authorization')).toBe('Bearer private-token');
    protectedRequest.flush([]);
    for (const url of [
      `${base}/productos`,
      'https://other.example/api/personal/pedidos',
      '/api-extra/personal',
    ]) {
      client.get(url).subscribe();
      const request = http.expectOne(url);
      expect(request.request.headers.has('Authorization')).toBe(false);
      request.flush([]);
    }
    client.post(`${base}/pedidos`, {}).subscribe();
    const publicOrder = http.expectOne(`${base}/pedidos`);
    expect(publicOrder.request.headers.has('Authorization')).toBe(false);
    publicOrder.flush({});
  });

  it('keeps the session on 403 and preserves the ProblemDetail payload', () => {
    const problem = {
      status: 403,
      title: 'Acceso denegado',
      detail: 'No tienes permisos.',
      instance: '/api/admin/usuarios',
    };
    client
      .get(`${base}/admin/usuarios`)
      .subscribe({ error: (error) => expect(error.error).toEqual(problem) });
    http
      .expectOne(`${base}/admin/usuarios`)
      .flush(problem, { status: 403, statusText: 'Forbidden' });
    expect(auth.token).toBe('private-token');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('clears the session and redirects on a protected 401', () => {
    client.get(`${base}/personal/pedidos`).subscribe({ error: () => undefined });
    http
      .expectOne(`${base}/personal/pedidos`)
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.token).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/panel/pedidos' },
    });
  });

  it('does not sign out when a public login fails', () => {
    auth
      .login({ nombreUsuario: 'other', contrasena: 'wrong' })
      .subscribe({ error: () => undefined });
    http.expectOne(`${base}/auth/login`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.token).toBe('private-token');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('does not clear a new login when a previous session request later returns 401', () => {
    client.get(`${base}/personal/pedidos`).subscribe({ error: () => undefined });
    const oldRequest = http.expectOne(`${base}/personal/pedidos`);
    auth.login({ nombreUsuario: 'other', contrasena: 'Nueva2026!' }).subscribe();
    http.expectOne(`${base}/auth/login`).flush({
      token: 'new-session-token',
      expiresIn: 3600,
      tokenType: 'Bearer',
      usuario: {
        id: 2,
        nombreUsuario: 'other',
        nombre: 'Otro',
        apellido: 'Usuario',
        correo: 'otro@example.com',
        rol: 'VENDEDOR',
        activo: true,
      },
    });
    oldRequest.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.token).toBe('new-session-token');
    expect(auth.user()?.id).toBe(2);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('does not clear a local session for a 401 from a different server', () => {
    client.get('https://other.example/api/private').subscribe({ error: () => undefined });
    const request = http.expectOne('https://other.example/api/private');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.token).toBe('private-token');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('also clears the session when the locally expired token is omitted from a request', () => {
    const now = Date.now();
    const clock = vi.spyOn(Date, 'now').mockReturnValue(now + 3_600_001);
    try {
      client.get(`${base}/personal/pedidos`).subscribe({ error: () => undefined });
      const request = http.expectOne(`${base}/personal/pedidos`);
      expect(request.request.headers.has('Authorization')).toBe(false);
      request.flush({}, { status: 401, statusText: 'Unauthorized' });
      expect(auth.user()).toBeNull();
      expect(auth.isAuthenticated()).toBe(false);
      expect(navigate).toHaveBeenCalled();
    } finally {
      clock.mockRestore();
    }
  });

  it('formats connection and validation errors without depending on backend stack traces', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toContain('conectar');
    expect(
      errorMessage(
        new HttpErrorResponse({
          status: 400,
          error: {
            detail: 'Revisa los campos.',
            errores: [{ campo: 'correo', mensaje: 'Correo inválido.' }],
          },
        }),
      ),
    ).toBe('Revisa los campos. correo: Correo inválido.');
    expect(
      errorMessage(new HttpErrorResponse({ status: 500, error: '<html>server stack</html>' })),
    ).not.toContain('server stack');
  });
});
