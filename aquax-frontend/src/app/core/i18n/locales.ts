import { registerLocaleData } from '@angular/common';
import localeEsPe from '@angular/common/locales/es-PE';
import localePtBr from '@angular/common/locales/pt';
import localeEn from '@angular/common/locales/en';

registerLocaleData(localeEsPe);
registerLocaleData(localePtBr, 'pt-BR');
registerLocaleData(localeEn, 'en-US');
