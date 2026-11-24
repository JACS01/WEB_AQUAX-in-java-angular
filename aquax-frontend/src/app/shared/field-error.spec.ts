import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, Validators } from '@angular/forms';
import { FieldError } from './field-error';

@Component({
  imports: [FieldError],
  template:
    '<app-field-error [control]="control" /><button (click)="control.markAsTouched()">Validar</button>',
})
class FormHost {
  readonly control = new FormControl('', Validators.required);
}

describe('FieldError accessibility', () => {
  it('announces validation after a template event changes the existing control', async () => {
    TestBed.configureTestingModule({ imports: [FormHost] });
    const fixture = TestBed.createComponent(FormHost);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
    fixture.nativeElement.querySelector('button').click();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'Este campo es obligatorio.',
    );
  });
});
