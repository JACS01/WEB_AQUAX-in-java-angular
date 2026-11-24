import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { apiRequest } from './api-url';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const { isApi, isPublic } = apiRequest(request);
  if (!isApi || isPublic) return next(request);
  const token = inject(AuthService).token;
  return next(
    token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request,
  );
};
