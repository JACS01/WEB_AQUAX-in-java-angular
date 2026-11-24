import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslationService } from '../core/i18n/translation.service';
@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  host: { '[attr.lang]': 'i18n.language()' },
  template:
    '<main id="main-content" class="not-found"><span class="eyebrow">AQUAX / 404</span><h1>{{ i18n.t("notFound.title") }}</h1><p class="muted">{{ i18n.t("notFound.description") }}</p><a class="button" routerLink="/">{{ i18n.t("notFound.back") }}</a></main>',
})
export class NotFoundPage {
  readonly i18n = inject(TranslationService);
}
