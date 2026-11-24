import { Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { TranslationService } from './core/i18n/translation.service';

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
  host: { '[attr.lang]': 'i18n.language()' },
})
export class App {
  readonly i18n = inject(TranslationService);
  readonly router = inject(Router);
}
