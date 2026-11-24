import { effect, inject, Injectable, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { TranslationKey, TranslationService } from './translation.service';

const titles: Record<string, TranslationKey> = {
  'AquaX · Agua que cuida': 'titles.home',
  'Iniciar sesión · AquaX': 'titles.login',
  'Resumen · AquaX': 'titles.dashboard',
  'Nuevo pedido · AquaX': 'titles.newOrder',
  'Pedidos · AquaX': 'titles.orders',
  'Reportes · AquaX': 'titles.reports',
  'Notificaciones · AquaX': 'titles.notifications',
  'Mi perfil · AquaX': 'titles.profile',
  'Preferencias · AquaX': 'titles.preferences',
  'Usuarios · AquaX': 'titles.users',
  'Empleados · AquaX': 'titles.employees',
  'Empresa · AquaX': 'titles.company',
  'Página no encontrada · AquaX': 'titles.notFound',
};

@Injectable()
export class LocalizedTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);
  private readonly i18n = inject(TranslationService);
  private readonly current = signal<string | undefined>(undefined);

  constructor() {
    super();
    effect(() => this.render());
  }

  override updateTitle(snapshot: RouterStateSnapshot): void {
    this.current.set(this.buildTitle(snapshot));
    this.render();
  }

  private render(): void {
    const title = this.current();
    if (title)
      this.title.setTitle(Object.hasOwn(titles, title) ? this.i18n.t(titles[title]) : title);
  }
}
