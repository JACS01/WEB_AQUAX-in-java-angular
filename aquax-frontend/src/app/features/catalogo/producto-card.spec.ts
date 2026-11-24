import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ProductoCard } from './producto-card';

@Component({
  imports: [ProductoCard],
  template:
    '<app-producto-card [producto]="producto" [cantidad]="cantidad()" (cantidadChange)="cantidad.set($event)" />',
})
class CatalogoHost {
  readonly producto = {
    id: 7,
    nombre: 'Bidón familiar',
    descripcion: 'Agua para tu hogar',
    precio: 12.5,
  };
  readonly cantidad = signal(1);
}

describe('ProductoCard communication', () => {
  beforeEach(() => TestBed.configureTestingModule({ imports: [CatalogoHost] }));

  it('asocia cada fotografía por producto aunque cambie el orden del catálogo', () => {
    const fixture = TestBed.createComponent(ProductoCard);
    for (const [nombre, filename] of [
      ['Caja 20L', 'caja.png'],
      ['Paquete 15 x 500 ml', 'botellas.png'],
      ['Bidón 20L', 'bidon.png'],
    ]) {
      fixture.componentRef.setInput('producto', {
        id: 7,
        nombre,
        descripcion: 'Agua AquaX',
        precio: 10,
      });
      fixture.componentRef.setInput('indice', 2);
      fixture.detectChanges();
      const image: HTMLImageElement = fixture.nativeElement.querySelector('img');
      expect(image.getAttribute('src')).toBe(`img/${filename}`);
      expect(image.alt).toBe(nombre);
    }
    fixture.componentRef.setInput('producto', {
      id: 8,
      nombre: 'Otro producto',
      descripcion: '',
      precio: 5,
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(fixture.nativeElement.querySelector('.product-placeholder')).not.toBeNull();
  });

  it('renders parent input and sends quantity events back to the parent cart', async () => {
    const fixture = TestBed.createComponent(CatalogoHost);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h3')?.textContent).toContain('Bidón familiar');
    element
      .querySelector<HTMLButtonElement>('[aria-label="Agregar una unidad de Bidón familiar"]')!
      .click();
    await fixture.whenStable();
    expect(fixture.componentInstance.cantidad()).toBe(2);
    expect(element.querySelector<HTMLInputElement>('input')!.value).toBe('2');
    element
      .querySelector<HTMLButtonElement>('[aria-label="Quitar una unidad de Bidón familiar"]')!
      .click();
    await fixture.whenStable();
    expect(fixture.componentInstance.cantidad()).toBe(1);
  });

  it('rejects fractional, negative, excessive and empty user input without changing the cart', async () => {
    const fixture = TestBed.createComponent(CatalogoHost);
    await fixture.whenStable();
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    for (const value of ['1.5', '-1', '10000', '']) {
      input.value = value;
      input.dispatchEvent(new Event('change', { bubbles: true }));
      await fixture.whenStable();
      expect(fixture.componentInstance.cantidad()).toBe(1);
      expect(input.value).toBe('1');
    }
    input.value = '4';
    input.dispatchEvent(new Event('change', { bubbles: true }));
    await fixture.whenStable();
    expect(fixture.componentInstance.cantidad()).toBe(4);
  });

  it('disables decrement at zero and increment at the maximum quantity', async () => {
    const fixture = TestBed.createComponent(CatalogoHost);
    fixture.componentInstance.cantidad.set(0);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    const minus = element.querySelector<HTMLButtonElement>(
      '[aria-label="Quitar una unidad de Bidón familiar"]',
    )!;
    expect(minus.disabled).toBe(true);
    minus.click();
    expect(fixture.componentInstance.cantidad()).toBe(0);
    fixture.componentInstance.cantidad.set(9999);
    await fixture.whenStable();
    const plus = element.querySelector<HTMLButtonElement>(
      '[aria-label="Agregar una unidad de Bidón familiar"]',
    )!;
    expect(plus.disabled).toBe(true);
    plus.click();
    expect(fixture.componentInstance.cantidad()).toBe(9999);
  });
});
