import { HttpErrorResponse } from '@angular/common/http';

export interface FieldError {
  campo: string;
  mensaje: string;
}

export function fieldErrors(error: unknown): FieldError[] {
  const errors = error instanceof HttpErrorResponse ? error.error?.errores : undefined;
  return Array.isArray(errors)
    ? errors.filter(
        (item): item is FieldError =>
          typeof item?.campo === 'string' && typeof item?.mensaje === 'string',
      )
    : [];
}

export function errorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse))
    return 'No se pudo completar la operación. Inténtalo nuevamente.';
  if (error.status === 0)
    return 'No se pudo conectar con AquaX. Comprueba que el servidor esté disponible y revisa tu conexión.';
  const detail = error.error?.detail;
  const fields = fieldErrors(error)
    .map((field) => `${field.campo}: ${field.mensaje}`)
    .join(' ');
  if (typeof detail === 'string' && detail.trim()) return fields ? `${detail} ${fields}` : detail;
  if (fields) return fields;
  switch (error.status) {
    case 400:
      return 'Revisa los datos y parámetros enviados.';
    case 401:
      return 'La sesión ha vencido o las credenciales son incorrectas. Inicia sesión nuevamente.';
    case 403:
      return 'Tu cuenta no tiene permiso para realizar esta operación.';
    case 404:
      return 'No se encontró el recurso solicitado.';
    case 409:
      return 'La operación entra en conflicto con los datos actuales.';
    case 503:
      return 'El servicio no está disponible. Revisa la configuración del servidor.';
    default:
      return 'No se pudo completar la operación. Inténtalo nuevamente.';
  }
}
