import { HttpRequest } from '@angular/common/http';
import { environment } from '../../../environments/environment';

/** Match an exact origin and path boundary so a token cannot escape to unrelated URLs. */
export function apiRequest(request: HttpRequest<unknown>): { isApi: boolean; isPublic: boolean } {
  const origin = typeof location !== 'undefined' ? location.origin : 'http://localhost';
  const base = new URL(environment.apiUrl, origin);
  const target = new URL(request.url, origin);
  const path = base.pathname.replace(/\/$/, '');
  const isApi =
    target.origin === base.origin &&
    (target.pathname === path || target.pathname.startsWith(`${path}/`));
  const relativePath = target.pathname.slice(path.length).replace(/\/$/, '');
  const isPublic =
    (request.method === 'GET' && relativePath === '/productos') ||
    (request.method === 'POST' && ['/auth/login', '/pedidos'].includes(relativePath));
  return { isApi, isPublic };
}
