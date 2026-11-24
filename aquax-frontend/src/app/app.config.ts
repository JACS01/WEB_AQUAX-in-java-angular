import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import {
  provideRouter,
  TitleStrategy,
  withComponentInputBinding,
  withInMemoryScrolling,
} from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import './core/i18n/locales';
import { LocalizedTitleStrategy } from './core/i18n/localized-title.strategy';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { apiErrorInterceptor } from './core/interceptors/api-error.interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: LOCALE_ID, useValue: 'es-PE' },
    { provide: TitleStrategy, useClass: LocalizedTitleStrategy },
    provideHttpClient(withInterceptors([authInterceptor, apiErrorInterceptor])),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled', anchorScrolling: 'enabled' }),
    ),
  ],
};
