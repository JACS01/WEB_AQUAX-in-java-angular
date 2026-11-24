import { TranslationService } from '../../core/i18n/translation.service';
import { Component, inject } from '@angular/core';
import { PedidoForm } from './pedido-form';
@Component({
  host: { '[attr.lang]': 'i18n.language()' },
  selector: 'app-nuevo-pedido',
  imports: [PedidoForm],
  template:
    '<div class="page-heading"><div><span class="eyebrow">{{ i18n.t("orderForm.staffEyebrow") }}</span><h1>{{ i18n.t("orderForm.staffTitle") }}</h1><p class="muted">{{ i18n.t("orderForm.staffDescription") }}</p></div></div><app-pedido-form [personal]="true" />',
})
export class NuevoPedidoPage {
  readonly i18n = inject(TranslationService);
}
