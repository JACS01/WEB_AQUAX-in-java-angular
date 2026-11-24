import { DatePipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { EMPTY, Observable, catchError, filter, finalize, switchMap } from 'rxjs';
import { Empleado, Pagina, RolUsuario, Usuario } from '../../core/models';
import { TranslationService } from '../../core/i18n/translation.service';
import { AuthService } from '../../core/auth/auth.service';
import { AdminService } from './admin.service';
import { AdminFieldError } from './field-error';
import { integerId, notBlank, passwordBytes } from './form-validation';
import { TableQueryState } from '../../shared/table-query-state';
import { adminSortOptions } from './sort-options';
import { isoDate, oneOf } from '../../shared/validators';

@Component({
  selector: 'app-empleados-page',
  imports: [ReactiveFormsModule, AdminFieldError, DatePipe],
  templateUrl: './empleados.page.html',
  styleUrl: './admin.scss',
  host: { '[attr.lang]': 'i18n.language()' },
  providers: [TableQueryState],
})
export class EmpleadosPage {
  readonly i18n = inject(TranslationService);
  private readonly api = inject(AdminService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly destroyRef = inject(DestroyRef);
  readonly table = inject(TableQueryState);
  readonly sortOptions = computed(() =>
    adminSortOptions(
      [
        ['nombre', this.i18n.t('admin.name')],
        ['apellido', this.i18n.t('admin.surname')],
        ['nombreUsuario', this.i18n.t('admin.username')],
        ['correo', this.i18n.t('admin.email')],
        ['dni', this.i18n.t('admin.dni')],
        ['fechaIngreso', this.i18n.t('admin.startDate')],
        ['rol', this.i18n.t('admin.role')],
        ['activo', this.i18n.t('admin.status')],
        ['id', this.i18n.t('admin.identifier')],
      ],
      this.i18n.t('admin.ascending'),
      this.i18n.t('admin.descending'),
    ),
  );
  readonly pagina = signal<Pagina<Empleado> | null>(null);
  readonly cuentas = signal<Usuario[]>([]);
  readonly loading = signal(false);
  readonly accountsLoading = signal(false);
  readonly accountsLoaded = signal(false);
  readonly saving = signal(false);
  readonly mode = signal<'closed' | 'link' | 'create' | 'edit'>('closed');
  readonly selected = signal<Empleado | null>(null);
  readonly deleting = signal<Empleado | null>(null);
  readonly error = signal('');
  readonly formError = signal('');
  readonly success = signal('');
  readonly search = this.table.search;
  readonly linkForm = this.fb.group({
    usuarioId: [0, [Validators.required, integerId]],
    dni: ['', [Validators.required, Validators.pattern('[0-9]{8}')]],
    fechaIngreso: ['', isoDate],
  });
  readonly createForm = this.fb.group({
    nombreUsuario: ['', [Validators.required, Validators.pattern('[a-zA-Z0-9._-]{3,50}')]],
    nombre: ['', [notBlank, Validators.maxLength(100)]],
    apellido: ['', [notBlank, Validators.maxLength(100)]],
    correo: ['', [notBlank, Validators.email, Validators.maxLength(150)]],
    contrasena: ['', [notBlank, Validators.minLength(8), Validators.maxLength(72), passwordBytes]],
    rol: this.fb.control<RolUsuario>('VENDEDOR', [
      Validators.required,
      oneOf(['ADMINISTRADOR', 'VENDEDOR']),
    ]),
    telefono: ['', [notBlank, Validators.maxLength(15)]],
    dni: ['', [Validators.required, Validators.pattern('[0-9]{8}')]],
    fechaIngreso: ['', isoDate],
  });
  readonly laborForm = this.fb.group({
    dni: ['', [Validators.required, Validators.pattern('[0-9]{8}')]],
    fechaIngreso: ['', [Validators.required, isoDate]],
  });
  readonly profileForm = this.fb.group({
    nombre: ['', [notBlank, Validators.maxLength(100)]],
    apellido: ['', [notBlank, Validators.maxLength(100)]],
    correo: ['', [notBlank, Validators.email, Validators.maxLength(150)]],
    telefono: ['', [notBlank, Validators.maxLength(15)]],
  });
  readonly role = this.fb.control<RolUsuario>('VENDEDOR', [
    Validators.required,
    oneOf(['ADMINISTRADOR', 'VENDEDOR']),
  ]);

  constructor() {
    this.table.initialize({
      defaultSort: 'nombre,asc',
      allowedSortFields: [
        'id',
        'dni',
        'fechaIngreso',
        'nombreUsuario',
        'nombre',
        'apellido',
        'correo',
        'rol',
        'activo',
      ],
    });
    this.table.changes$
      .pipe(
        switchMap((query) => {
          this.loading.set(true);
          this.error.set('');
          return this.api.empleados(query.page, query.texto, query.size, query.sort).pipe(
            filter((data) => {
              const lastPage = Math.max(data.page.totalPages - 1, 0);
              if (query.page <= lastPage) return true;
              this.table.goTo(lastPage);
              return false;
            }),
            catchError((error) => {
              this.pagina.set(null);
              this.error.set(this.i18n.error(error));
              return EMPTY;
            }),
            finalize(() => this.loading.set(false)),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((data) => this.pagina.set(data));
  }

  load(page?: number) {
    if (page !== undefined && page !== this.table.page()) this.table.goTo(page);
    else this.table.refresh();
  }

  changeSize(event: Event) {
    this.table.setSize(Number((event.target as HTMLSelectElement).value));
  }
  changeSort(event: Event) {
    this.table.setSort((event.target as HTMLSelectElement).value);
  }

  open(mode: 'link' | 'create') {
    this.formError.set('');
    this.mode.set(mode);
    this.selected.set(null);
    this.deleting.set(null);
    if (mode === 'link') {
      this.accountsLoaded.set(false);
      this.cuentas.set([]);
      this.accountsLoading.set(true);
      this.api
        .cuentas()
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          finalize(() => this.accountsLoading.set(false)),
        )
        .subscribe({
          next: (data) => {
            this.cuentas.set(data);
            this.accountsLoaded.set(true);
          },
          error: (error) => this.formError.set(this.i18n.error(error)),
        });
    }
  }

  edit(empleado: Empleado) {
    this.selected.set(empleado);
    this.mode.set('edit');
    this.deleting.set(null);
    this.formError.set('');
    this.laborForm.reset({ dni: empleado.dni, fechaIngreso: empleado.fechaIngreso });
    this.profileForm.reset({
      nombre: empleado.nombre,
      apellido: empleado.apellido,
      correo: empleado.correo,
      telefono: empleado.telefono ?? '',
    });
    this.role.setValue(empleado.rol);
  }

  link() {
    this.linkForm.markAllAsTouched();
    if (this.linkForm.invalid || this.saving() || this.accountsLoading() || !this.accountsLoaded())
      return;
    const { fechaIngreso, ...datos } = this.linkForm.getRawValue();
    this.mutate(
      this.api.vincular({ ...datos, ...(fechaIngreso ? { fechaIngreso } : {}) }),
      this.i18n.t('admin.recordCreated'),
      true,
    );
  }

  create() {
    this.createForm.markAllAsTouched();
    if (this.createForm.invalid || this.saving()) return;
    const { dni, fechaIngreso, telefono, ...usuario } = this.createForm.getRawValue();
    this.mutate(
      this.api.crearEmpleado({ usuario, dni, telefono, ...(fechaIngreso ? { fechaIngreso } : {}) }),
      this.i18n.t('admin.employeeAccountCreated'),
      true,
    );
  }

  saveLabor() {
    this.laborForm.markAllAsTouched();
    const empleado = this.selected();
    if (!empleado || this.laborForm.invalid || this.saving()) return;
    this.mutate(
      this.api.actualizarLaboral(empleado.id, this.laborForm.getRawValue()),
      this.i18n.t('admin.employmentUpdated'),
    );
  }

  saveProfile() {
    this.profileForm.markAllAsTouched();
    const empleado = this.selected();
    if (!empleado || this.profileForm.invalid || this.saving()) return;
    this.mutate(
      this.api.actualizarPerfil(empleado.id, this.profileForm.getRawValue()),
      this.i18n.t('admin.profileUpdated'),
    );
  }

  saveRole() {
    this.role.markAsTouched();
    const empleado = this.selected();
    if (!empleado || this.saving() || this.role.invalid) return;
    this.mutate(
      this.api.cambiarRol(empleado.id, this.role.value),
      this.i18n.t('admin.roleUpdated'),
    );
  }

  toggle(empleado: Empleado) {
    if (this.saving()) return;
    this.mutate(
      this.api.activarEmpleado(empleado.id, !empleado.activo),
      this.i18n.t(empleado.activo ? 'admin.accountDeactivated' : 'admin.accountActivated'),
    );
  }

  remove() {
    const empleado = this.deleting();
    if (!empleado || this.saving()) return;
    this.saving.set(true);
    this.error.set('');
    this.api
      .eliminarEmpleado(empleado.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: () => {
          this.deleting.set(null);
          this.mode.set('closed');
          this.selected.set(null);
          this.success.set(this.i18n.t('admin.recordDeleted'));
          this.load(this.pagina()?.page.number ?? 0);
        },
        error: (error) => this.error.set(this.i18n.error(error)),
      });
  }

  private mutate(operation: Observable<Empleado>, message: string, close = false) {
    this.saving.set(true);
    this.error.set('');
    this.formError.set('');
    this.success.set('');
    operation
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (empleado) => {
          if (empleado.usuarioId === this.auth.user()?.id) {
            if (!empleado.activo) {
              this.auth.logout();
              return;
            }
            this.auth.updateUser({
              nombre: empleado.nombre,
              apellido: empleado.apellido,
              correo: empleado.correo,
              rol: empleado.rol,
              activo: empleado.activo,
            });
            if (empleado.rol !== 'ADMINISTRADOR') {
              void this.router.navigateByUrl('/panel');
              return;
            }
          }
          this.success.set(message);
          if (close) {
            this.mode.set('closed');
            this.createForm.reset();
            this.linkForm.reset();
          } else if (this.selected()?.id === empleado.id) this.selected.set(empleado);
          this.load(close ? 0 : (this.pagina()?.page.number ?? 0));
        },
        error: (error) => {
          if (this.mode() === 'closed') this.error.set(this.i18n.error(error));
          else this.formError.set(this.i18n.error(error));
        },
      });
  }
}
