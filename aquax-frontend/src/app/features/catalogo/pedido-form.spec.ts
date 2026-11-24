import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { PedidoForm } from './pedido-form';

describe('PedidoForm', () => {
  let fixture: ComponentFixture<PedidoForm>;
  let component: PedidoForm;
  let http: HttpTestingController;
  const base = environment.apiUrl;
  const customer = {
    nombreCliente: 'Ana Pérez',
    telefonoCliente: '999888777',
    direccionEntrega: 'Av. Lima 123',
    observaciones: 'Puerta azul',
  };
  const products = [
    { id: 1, nombre: 'Bidón', descripcion: 'Agua', precio: 0.1 },
    { id: 2, nombre: 'Botella', descripcion: 'Agua', precio: 0.2 },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PedidoForm],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PedidoForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
    http.expectOne(`${base}/productos`).flush(products);
    fixture.detectChanges();
  });
  afterEach(() => http.verify());

  it('rejects fractional, negative and excessive quantities and calculates integer cents', () => {
    component.setQuantity(1, 2);
    component.setQuantity(2, 3);
    expect(component.units()).toBe(5);
    expect(component.total()).toBe(0.8);
    for (const invalid of [1.5, -1, 10000, Number.NaN]) component.setQuantity(1, invalid);
    expect(component.quantity(1)).toBe(2);
    const input = document.createElement('input');
    input.value = '2.5';
    component.changeQuantity(1, { target: input } as unknown as Event);
    expect(input.value).toBe('2');
  });

  it('blocks an empty cart and invalid delivery information without a POST', () => {
    component.form.setValue(customer);
    component.submit();
    http.expectNone(`${base}/pedidos`);
    component.setQuantity(1, 1);
    component.form.controls.nombreCliente.setValue('   ');
    component.submit();
    http.expectNone(`${base}/pedidos`);
    expect(component.form.controls.nombreCliente.touched).toBe(true);
  });

  it('posts selected product IDs and quantities only once through pending and success states', () => {
    component.form.setValue(customer);
    component.setQuantity(2, 3);
    component.submit();
    component.submit();
    const request = http.expectOne(`${base}/pedidos`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      ...customer,
      detalles: [{ productoId: 2, cantidad: 3 }],
    });
    expect(component.busy()).toBe(true);
    // The confirmed amount comes from the server, independently of the displayed estimate.
    request.flush({
      id: 7,
      numeroPedido: 'AQX-0007',
      cantidadProducto: 3,
      total: 0.9,
      direccionEntrega: customer.direccionEntrega,
    });
    expect(component.busy()).toBe(false);
    expect(component.success()?.total).toBe(0.9);
    component.submit();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('AQX-0007');
    http.expectNone(`${base}/pedidos`);
  });

  it('uses the protected personal endpoint when embedded in the staff panel', () => {
    fixture.componentRef.setInput('personal', true);
    component.form.setValue(customer);
    component.setQuantity(1, 1);
    component.submit();
    http.expectNone(`${base}/pedidos`);
    http.expectOne(`${base}/personal/pedidos`).flush({ id: 1 });
  });

  it('retains the entered order after an HTTP failure and never automatically retries creation', () => {
    component.form.setValue(customer);
    component.setQuantity(1, 2);
    component.submit();
    http
      .expectOne(`${base}/pedidos`)
      .flush(
        { detail: 'Servicio temporalmente indisponible.' },
        { status: 500, statusText: 'Internal Server Error' },
      );
    expect(component.busy()).toBe(false);
    expect(component.success()).toBeNull();
    expect(component.form.getRawValue()).toEqual(customer);
    expect(component.quantity(1)).toBe(2);
    expect(component.error()).toContain('Servicio temporalmente');
    http.expectNone(`${base}/pedidos`);
  });

  it('clears the previous order and reloads the catalogue only on explicit new order', () => {
    component.form.setValue(customer);
    component.setQuantity(1, 2);
    component.submit();
    http.expectOne(`${base}/pedidos`).flush({ id: 1 });
    component.newOrder();
    expect(component.success()).toBeNull();
    expect(component.units()).toBe(0);
    expect(component.form.controls.nombreCliente.value).toBe('');
    http.expectOne(`${base}/productos`).flush(products);
    http.expectNone(`${base}/pedidos`);
  });
});
