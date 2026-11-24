import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, mergeMap, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { apiRequest } from './api-url';

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const { isApi, isPublic } = apiRequest(request);
  const tokenAtRequest = auth.token;
  return next(request).pipe(
    catchError((error: unknown) => {
      if (!isApi || !(error instanceof HttpErrorResponse)) return throwError(() => error);
      // A response from a previous session must not sign out a newer login.
      if (!isPublic && error.status === 401 && (!auth.token || auth.token === tokenAtRequest)) {
        auth.logout();
      }
      // CSV endpoints return Blob even when the server sends an application/problem+json error.
      if (
        typeof Blob !== 'undefined' &&
        error.error instanceof Blob &&
        error.error.type.includes('json')
      ) {
        return from(error.error.text()).pipe(
          mergeMap((body) => {
            let problem: unknown;
            try {
              problem = JSON.parse(body);
            } catch {
              return throwError(() => error);
            }
            return throwError(
              () =>
                new HttpErrorResponse({
                  error: problem,
                  headers: error.headers,
                  status: error.status,
                  statusText: error.statusText,
                  url: error.url ?? undefined,
                }),
            );
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};
