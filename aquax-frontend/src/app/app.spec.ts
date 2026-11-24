import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

describe('application routes', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
  });
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    sessionStorage.clear();
  });

  it('renders login instead of administrative content for an unauthenticated deep link', async () => {
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/panel/usuarios');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(router.url).toBe('/login?returnUrl=%2Fpanel%2Fusuarios');
    expect(fixture.nativeElement.querySelector('input[autocomplete="username"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-usuarios')).toBeNull();
  });

  it('provides a usable return to the catalogue for an unknown URL', async () => {
    const fixture = TestBed.createComponent(App);
    await TestBed.inject(Router).navigateByUrl('/ruta-inexistente');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('main h1')?.textContent).toContain(
      'no está disponible',
    );
    expect(fixture.nativeElement.querySelector('main a')?.getAttribute('href')).toBe('/');
  });
});
