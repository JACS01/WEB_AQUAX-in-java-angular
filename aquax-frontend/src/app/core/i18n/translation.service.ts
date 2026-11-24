import { computed, Injectable, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { errorMessage, fieldErrors } from '../interceptors/api-error';
import './locales';
import es from './es.json';
import en from './en.json';
import pt from './pt.json';

export type Language = 'es' | 'en' | 'pt';
export type TranslationKey = keyof typeof es;

const dictionaries: Record<Language, Record<TranslationKey, string>> = { es, en, pt };
const locales: Record<Language, string> = { es: 'es-PE', en: 'en-US', pt: 'pt-BR' };

const labels: Record<string, TranslationKey> = {
  NUEVO: 'labels.new',
  EN_CAMINO: 'labels.onTheWay',
  ENTREGADO: 'labels.delivered',
  PENDIENTE: 'labels.pending',
  PAGADO: 'labels.paid',
  CLIENTE: 'labels.customer',
  TRABAJADOR: 'labels.staff',
  ADMINISTRADOR: 'layout.admin',
  VENDEDOR: 'layout.seller',
  creacion: 'labels.creation',
  estadoEntrega: 'labels.delivery',
  estadoPago: 'labels.payment',
  'N/A': 'labels.notApplicable',
};

// Match known server messages for presentation only. HTTP payloads are never modified.
const serverMessages: Record<string, TranslationKey> = {
  'El nombre de usuario o correo ya está registrado.': 'serverErrors.duplicateAccount',
  'El correo ya está registrado por otra cuenta.': 'serverErrors.duplicateEmail',
  'El correo ya está registrado.': 'serverErrors.duplicateEmail',
  'La nueva contraseña y su confirmación no coinciden.': 'serverErrors.passwordMismatch',
  'La contraseña actual es incorrecta.': 'serverErrors.currentPassword',
  'Debe permanecer al menos un administrador activo.': 'serverErrors.lastAdmin',
  'El DNI ya está registrado por otro empleado.': 'serverErrors.duplicateDni',
  'La cuenta ya está vinculada a un empleado.': 'serverErrors.linkedAccount',
  'La fecha desde debe ser anterior o igual a la fecha hasta.': 'serverErrors.dateRange',
  'La contraseña debe ocupar como máximo 72 bytes en UTF-8.': 'validation.passwordBytes',
  'La nueva contraseña debe ocupar como máximo 72 bytes en UTF-8.': 'validation.passwordBytes',
  'Usuario o contraseña incorrectos.': 'serverErrors.credentials',
};

const fieldNames: Record<string, TranslationKey> = {
  nombre: 'fields.name',
  apellido: 'fields.surname',
  nombreUsuario: 'fields.username',
  correo: 'fields.email',
  telefono: 'fields.phone',
  dni: 'fields.dni',
  contrasena: 'fields.password',
  contrasenaActual: 'fields.currentPassword',
  nuevaContrasena: 'fields.newPassword',
  confirmarContrasena: 'fields.confirmPassword',
  nombreCliente: 'fields.customer',
  telefonoCliente: 'fields.phone',
  direccionEntrega: 'fields.address',
  observaciones: 'fields.notes',
  detalles: 'fields.products',
  cantidad: 'fields.quantity',
  productoId: 'fields.product',
  usuarioId: 'fields.account',
  rol: 'fields.role',
  fechaIngreso: 'fields.startDate',
  idioma: 'preferences.language',
  nivelZoom: 'preferences.zoomLevel',
  filtroColor: 'preferences.colorFilter',
  filtroColorActivo: 'preferences.applyFilter',
  desde: 'fields.from',
  hasta: 'fields.to',
  razonSocial: 'fields.companyName',
  ruc: 'fields.ruc',
  direccion: 'fields.address',
  telefono1: 'fields.phone',
  telefono2: 'fields.phone',
  registroSanitario: 'fields.healthRegistration',
  slogan: 'fields.slogan',
};

/** Texts live in the JSON catalogs; the account preference selects a catalog. */
@Injectable({ providedIn: 'root' })
export class TranslationService {
  private readonly currentLanguage = signal<Language>('es');
  readonly language = this.currentLanguage.asReadonly();
  readonly locale = computed(() => locales[this.language()]);

  use(language: string): void {
    const supported = language === 'en' || language === 'pt' ? language : 'es';
    this.currentLanguage.set(supported);
  }

  t(key: TranslationKey, params: Record<string, string | number> = {}): string {
    const template = dictionaries[this.language()][key] ?? es[key] ?? key;
    return template.replace(/\{(\w+)\}/g, (placeholder, name: string) =>
      Object.hasOwn(params, name) ? String(params[name]) : placeholder,
    );
  }

  /** Only pass controlled enum/history values here, never customer-entered text. */
  label(value: string): string {
    return Object.hasOwn(labels, value) ? this.t(labels[value]) : value;
  }

  error(error: unknown): string {
    // Preserve useful original details in the original language.
    if (this.language() === 'es') return errorMessage(error);
    if (!(error instanceof HttpErrorResponse)) return this.t('errors.generic');
    if (error.status === 0) return this.t('errors.network');
    const detail = error.error?.detail;
    if (typeof detail === 'string' && Object.hasOwn(serverMessages, detail)) {
      return this.t(serverMessages[detail]);
    }
    const statusKeys: Record<number, TranslationKey> = {
      400: 'errors.badRequest',
      401: 'errors.unauthorized',
      403: 'errors.forbidden',
      404: 'errors.notFound',
      409: 'errors.conflict',
      503: 'errors.unavailable',
    };
    const message = this.t(statusKeys[error.status] ?? 'errors.generic');
    const fields = [
      ...new Set(
        fieldErrors(error).flatMap(({ campo }) => {
          const normalized =
            campo
              .replace(/\[\d+\]/g, '')
              .split('.')
              .pop() ?? '';
          return Object.hasOwn(fieldNames, normalized) ? [this.t(fieldNames[normalized])] : [];
        }),
      ),
    ];
    return fields.length && error.status === 400
      ? `${message} ${this.t('errors.fields', { fields: fields.join(', ') })}`
      : message;
  }
}
