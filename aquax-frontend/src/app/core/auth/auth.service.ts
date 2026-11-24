import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, map, Observable, of, shareReplay, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginResponse, Usuario } from '../models';
import { PreferencesService } from '../preferences.service';

interface Session {
  token: string;
  expiresAt: number;
}
const SESSION_KEY = 'aquax.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly preferences = inject(PreferencesService);
  private readonly session = signal<Session | null>(this.restore());
  private validation?: Observable<boolean>;
  private validationToken?: string;
  readonly user = signal<Usuario | null>(null);
  readonly isAuthenticated = computed(() => this.user() !== null && this.session() !== null);
  readonly isAdmin = computed(() => this.isAuthenticated() && this.user()?.rol === 'ADMINISTRADOR');

  get token(): string | null {
    const session = this.session();
    return session && session.expiresAt > Date.now() ? session.token : null;
  }

  login(credentials: { nombreUsuario: string; contrasena: string }): Observable<Usuario> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, credentials).pipe(
      tap((response) => {
        const session = {
          token: response.token,
          expiresAt: Date.now() + response.expiresIn * 1000,
        };
        this.session.set(session);
        this.user.set(response.usuario);
        this.persist(session);
      }),
      map((response) => response.usuario),
    );
  }

  /** Resolve the current account from the server; a stored token never supplies a trusted role. */
  ensureSession(): Observable<boolean> {
    const token = this.token;
    if (!token) {
      this.logout(false);
      return of(false);
    }
    if (this.validation && this.validationToken === token) return this.validation;
    this.validationToken = token;
    this.validation = this.http.get<Usuario>(`${environment.apiUrl}/auth/me`).pipe(
      map((user) => {
        if (this.token !== token) return false;
        if (!user.activo) {
          this.logout(false);
          return false;
        }
        this.user.set(user);
        return true;
      }),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401 && this.token === token) {
          this.logout(false);
        }
        return of(false);
      }),
      finalize(() => {
        if (this.validationToken === token) {
          this.validation = undefined;
          this.validationToken = undefined;
        }
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.validation;
  }

  updateUser(changes: Partial<Usuario>): void {
    this.user.update((user) => (user ? { ...user, ...changes } : null));
  }

  logout(navigate = true): void {
    this.preferences.reset();
    this.session.set(null);
    this.user.set(null);
    this.validation = undefined;
    this.validationToken = undefined;
    try {
      sessionStorage.removeItem(SESSION_KEY);
    } catch {
      /* Storage may be disabled. */
    }
    if (navigate) {
      const returnUrl = this.router.url;
      void this.router.navigate(['/login'], {
        queryParams: returnUrl.startsWith('/panel') ? { returnUrl } : undefined,
      });
    }
  }

  private persist(session: Session): void {
    try {
      sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
    } catch {
      /* Keep an in-memory session. */
    }
  }

  private restore(): Session | null {
    try {
      const raw = sessionStorage.getItem(SESSION_KEY);
      if (!raw) return null;
      const session: Partial<Session> = JSON.parse(raw);
      if (
        typeof session.token === 'string' &&
        session.token.length > 0 &&
        typeof session.expiresAt === 'number' &&
        session.expiresAt > Date.now()
      ) {
        return { token: session.token, expiresAt: session.expiresAt };
      }
      sessionStorage.removeItem(SESSION_KEY);
    } catch {
      /* An invalid or unavailable store is treated as a signed-out session. */
    }
    return null;
  }
}
