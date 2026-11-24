import { TranslationService } from '../../core/i18n/translation.service';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PedidoForm } from './pedido-form';
import { Icon } from '../../shared/icon';
import { LanguageSwitcher } from '../../shared/language-switcher';
@Component({
  host: { '[attr.lang]': 'i18n.language()' },
  selector: 'app-catalogo',
  imports: [RouterLink, PedidoForm, Icon, LanguageSwitcher],
  templateUrl: './catalogo.page.html',
  styleUrl: './catalogo.page.scss',
})
export class CatalogoPage {
  readonly i18n = inject(TranslationService);
}
