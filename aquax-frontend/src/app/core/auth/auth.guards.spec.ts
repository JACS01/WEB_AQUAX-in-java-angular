import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { adminGuard, authGuard } from './auth.guards';

describe('route guards', () => {
  let http: HttpTestingController;
  let router: Router;
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/panel/usuarios' } as RouterStateSnapshot;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });
  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('redirects signed-out users with the internal destination preserved', () => {
    const result = TestBed.runInInjectionContext(() => authGuard(route, state)) as Observable<
      boolean | UrlTree
    >;
    result.subscribe((value) =>
      expect(router.serializeUrl(value as UrlTree)).toBe('/login?returnUrl=%2Fpanel%2Fusuarios'),
    );
    http.expectNone(`${environment.apiUrl}/auth/me`);
  });

  it('blocks a stored administrator whose current backend role is seller', () => {
    sessionStorage.setItem(
      'aquax.session',
      JSON.stringify({ token: 'token', expiresAt: Date.now() + 10000 }),
    );
    const result = TestBed.runInInjectionContext(() => adminGuard(route, state)) as Observable<
      boolean | UrlTree
    >;
    result.subscribe((value) => expect(router.serializeUrl(value as UrlTree)).toBe('/panel'));
    http.expectOne(`${environment.apiUrl}/auth/me`).flush({
      id: 1,
      nombreUsuario: 'ana',
      nombre: 'Ana',
      apellido: 'Pérez',
      correo: 'ana@example.com',
      activo: true,
      rol: 'VENDEDOR',
    });
  });

  it('allows administrators only after the server confirms their role', () => {
    sessionStorage.setItem(
      'aquax.session',
      JSON.stringify({ token: 'token', expiresAt: Date.now() + 10000 }),
    );
    const result = TestBed.runInInjectionContext(() => adminGuard(route, state)) as Observable<
      boolean | UrlTree
    >;
    result.subscribe((value) => expect(value).toBe(true));
    http.expectOne(`${environment.apiUrl}/auth/me`).flush({
      id: 1,
      nombreUsuario: 'ana',
      nombre: 'Ana',
      apellido: 'Pérez',
      correo: 'ana@example.com',
      activo: true,
      rol: 'ADMINISTRADOR',
    });
  });
});
