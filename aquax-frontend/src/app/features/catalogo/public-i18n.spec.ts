import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslationService } from '../../core/i18n/translation.service';
import { LoginPage } from '../auth/login.page';
import { CatalogoPage } from './catalogo.page';
import { PedidoForm } from './pedido-form';

const product = {
  id: 7,
  nombre: 'Bidón familiar',
  descripcion: 'Agua para tu hogar',
  precio: 12.5,
};
const customer = {
  nombreCliente: 'Ana Pérez',
  telefonoCliente: '999888777',
  direccionEntrega: 'Av. Los Álamos 123',
  observaciones: 'Portón azul',
};

describe('Idiomas públicos sin modificar datos ni repetir solicitudes', () => {
  let http: HttpTestingController;
  let i18n: TranslationService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [CatalogoPage, PedidoForm, LoginPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    i18n = TestBed.inject(TranslationService);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  function choose<T>(fixture: ComponentFixture<T>, language: string): void {
    const select: HTMLSelectElement = fixture.nativeElement.querySelector(
      'app-language-switcher select',
    );
    select.value = language;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  it('traduce catálogo y formulario en inglés y portugués conservando productos y entradas', () => {
    const fixture = TestBed.createComponent(CatalogoPage);
    fixture.detectChanges();
    http.expectOne('/api/productos').flush([product]);
    fixture.detectChanges();
    const name: HTMLInputElement = fixture.nativeElement.querySelector(
      '[formControlName="nombreCliente"]',
    );
    name.value = customer.nombreCliente;
    name.dispatchEvent(new Event('input'));
    choose(fixture, 'en');
    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Water that cares.');
    expect(fixture.nativeElement.textContent).toContain('Customer name');
    expect(fixture.nativeElement.querySelector('app-producto-card h3').textContent).toBe(
      product.nombre,
    );
    expect(name.value).toBe(customer.nombreCliente);
    expect(
      fixture.nativeElement.querySelector('app-producto-card input').getAttribute('aria-label'),
    ).toBe('Quantity of Bidón familiar');
    choose(fixture, 'pt');
    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Água que cuida.');
    expect(fixture.nativeElement.textContent).toContain('Nome do cliente');
    expect(fixture.nativeElement.querySelector('app-producto-card h3').textContent).toBe(
      product.nombre,
    );
    expect(name.value).toBe(customer.nombreCliente);
    http.expectNone('/api/productos');
  });

  it('traduce un fallo de catálogo ya visible al cambiar el selector sin repetir el GET', () => {
    const fixture = TestBed.createComponent(CatalogoPage);
    fixture.detectChanges();
    http.expectOne('/api/productos').error(new ProgressEvent('network'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'No se pudo conectar',
    );
    choose(fixture, 'en');
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      i18n.t('errors.network'),
    );
    choose(fixture, 'pt');
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      i18n.t('errors.network'),
    );
    http.expectNone('/api/productos');
  });

  it('traduce un error de registro existente sin alterar el pedido ni volver a enviarlo', () => {
    const fixture = TestBed.createComponent(PedidoForm);
    fixture.detectChanges();
    http.expectOne('/api/productos').flush([product]);
    const page = fixture.componentInstance;
    page.form.setValue(customer);
    page.setQuantity(product.id, 2);
    page.reviewOrder();
    page.submit();
    const request = http.expectOne('/api/pedidos');
    expect(request.request.body).toEqual({
      ...customer,
      detalles: [{ productoId: 7, cantidad: 2 }],
    });
    request.flush(
      { detail: 'Revisa el pedido enviado.' },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();
    expect(page.error()).toContain('Revisa el pedido enviado.');
    for (const language of ['en', 'pt']) {
      i18n.use(language);
      fixture.detectChanges();
      expect(page.error()).toBe(i18n.t('errors.badRequest'));
      expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
        page.error(),
      );
      expect(page.form.getRawValue()).toEqual(customer);
      expect(page.quantity(7)).toBe(2);
    }
    http.expectNone('/api/pedidos');
  });

  it('traduce también los errores locales de cantidades al cambiar el idioma', () => {
    const fixture = TestBed.createComponent(PedidoForm);
    fixture.detectChanges();
    http.expectOne('/api/productos').flush([product]);
    const page = fixture.componentInstance;
    page.form.setValue(customer);
    page.quantities.set({ 7: 1.5 });
    page.reviewOrder();
    expect(page.error()).toContain('cantidades enteras');
    i18n.use('en');
    expect(page.error()).toBe(i18n.t('orderForm.invalidQuantities'));
    i18n.use('pt');
    expect(page.error()).toBe(i18n.t('orderForm.invalidQuantities'));
    http.expectNone('/api/pedidos');
  });

  it('traduce login y su error existente sin reenviar credenciales', () => {
    const fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    page.form.setValue({ nombreUsuario: 'ana', contrasena: 'Prueba2026!' });
    page.submit();
    http
      .expectOne('/api/auth/login')
      .flush(
        { detail: 'Usuario o contraseña incorrectos.' },
        { status: 401, statusText: 'Unauthorized' },
      );
    fixture.detectChanges();
    expect(page.error()).toContain('Usuario o contraseña incorrectos.');
    choose(fixture, 'en');
    expect(fixture.nativeElement.querySelector('h2').textContent).toBe('Good to see you.');
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      i18n.t('serverErrors.credentials'),
    );
    expect(page.error()).toBe(i18n.t('serverErrors.credentials'));
    choose(fixture, 'pt');
    expect(fixture.nativeElement.querySelector('h2').textContent).toBe('Que bom ver você.');
    expect(page.error()).toBe(i18n.t('serverErrors.credentials'));
    expect(page.form.controls.nombreUsuario.value).toBe('ana');
    http.expectNone('/api/auth/login');
  });
});
