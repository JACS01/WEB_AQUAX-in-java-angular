import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { CuentaService } from '../features/cuenta/cuenta.service';
import { PreferencesService } from '../core/preferences.service';
import { Icon } from '../shared/icon';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslationService } from '../core/i18n/translation.service';
@Component({
  selector: 'app-panel-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, Icon],
  templateUrl: './panel.layout.html',
  host: { '[attr.lang]': 'i18n.language()' },
})
export class PanelLayout implements OnInit {
  readonly auth = inject(AuthService);
  readonly i18n = inject(TranslationService);
  private readonly cuenta = inject(CuentaService);
  private readonly prefs = inject(PreferencesService);
  private readonly destroyRef = inject(DestroyRef);
  readonly menuOpen = signal(false);
  readonly today = computed(() =>
    new Intl.DateTimeFormat(this.i18n.locale(), {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      timeZone: 'America/Lima',
    }).format(new Date()),
  );
  initials() {
    return (
      (this.auth.user()?.nombre[0] ?? '') + (this.auth.user()?.apellido[0] ?? '')
    ).toUpperCase();
  }
  ngOnInit() {
    const revision = this.prefs.revision;
    this.cuenta
      .preferencias()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        // A delayed initial read must not undo a more recently saved preference.
        next: (p) => {
          if (this.prefs.revision === revision) this.prefs.apply(p);
        },
        error: () => {
          if (this.prefs.revision === revision) this.prefs.reset();
        },
      });
  }
  logout() {
    this.prefs.reset();
    this.auth.logout();
  }
}
