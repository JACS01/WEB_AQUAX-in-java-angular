import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { EMPTY, catchError, filter, finalize, switchMap } from 'rxjs';
import { Pagina, RolUsuario, Usuario } from '../../core/models';
import { TranslationService } from '../../core/i18n/translation.service';
import { AuthService } from '../../core/auth/auth.service';
import { AdminService } from './admin.service';
import { AdminFieldError } from './field-error';
import { notBlank, passwordBytes } from './form-validation';
import { TableQueryState } from '../../shared/table-query-state';
import { adminSortOptions } from './sort-options';
import { oneOf } from '../../shared/validators';

@Component({
  selector: 'app-usuarios-page',
  imports: [ReactiveFormsModule, AdminFieldError],
  templateUrl: './usuarios.page.html',
  styleUrl: './admin.scss',
  host: { '[attr.lang]': 'i18n.language()' },
  providers: [TableQueryState],
})
export class UsuariosPage {
  readonly i18n = inject(TranslationService);
  private readonly api = inject(AdminService);
  private readonly auth = inject(AuthService);
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
        ['rol', this.i18n.t('admin.role')],
        ['activo', this.i18n.t('admin.status')],
        ['id', this.i18n.t('admin.identifier')],
      ],
      this.i18n.t('admin.ascending'),
      this.i18n.t('admin.descending'),
    ),
  );
  readonly pagina = signal<Pagina<Usuario> | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly editing = signal(false);
  readonly error = signal('');
  readonly formError = signal('');
  readonly success = signal('');
  readonly search = this.table.search;
  readonly form = this.fb.group({
    nombreUsuario: ['', [Validators.required, Validators.pattern('[a-zA-Z0-9._-]{3,50}')]],
    nombre: ['', [notBlank, Validators.maxLength(100)]],
    apellido: ['', [notBlank, Validators.maxLength(100)]],
    correo: ['', [notBlank, Validators.email, Validators.maxLength(150)]],
    contrasena: ['', [notBlank, Validators.minLength(8), Validators.maxLength(72), passwordBytes]],
    rol: this.fb.control<RolUsuario>('VENDEDOR', [
      Validators.required,
      oneOf(['ADMINISTRADOR', 'VENDEDOR']),
    ]),
  });

  constructor() {
    this.table.initialize({
      defaultSort: 'nombre,asc',
      allowedSortFields: ['id', 'nombreUsuario', 'nombre', 'apellido', 'correo', 'rol', 'activo'],
    });
    this.table.changes$
      .pipe(
        switchMap((query) => {
          this.error.set('');
          this.loading.set(true);
          return this.api.usuarios(query.page, query.texto, query.size, query.sort).pipe(
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

  create() {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.saving()) return;
    this.saving.set(true);
    this.formError.set('');
    this.success.set('');
    this.api
      .crearUsuario(this.form.getRawValue())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: () => {
          this.form.reset();
          this.editing.set(false);
          this.success.set(this.i18n.t('admin.accountCreated'));
          this.load(0);
        },
        error: (error) => this.formError.set(this.i18n.error(error)),
      });
  }

  toggle(usuario: Usuario) {
    if (this.saving()) return;
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    this.api
      .activarUsuario(usuario.id, !usuario.activo)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (actualizado) => {
          if (actualizado.id === this.auth.user()?.id) {
            if (!actualizado.activo) {
              this.auth.logout();
              return;
            }
            this.auth.updateUser(actualizado);
          }
          this.success.set(
            this.i18n.t(actualizado.activo ? 'admin.accountActivated' : 'admin.accountDeactivated'),
          );
          this.load(this.pagina()?.page.number ?? 0);
        },
        error: (error) => this.error.set(this.i18n.error(error)),
      });
  }
}
