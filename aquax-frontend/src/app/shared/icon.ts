import { Component, input } from '@angular/core';

const paths: Record<string, string> = {
  drop: 'M12 3C9 7 5 11 5 15a7 7 0 0 0 14 0c0-4-4-8-7-12Z M8 15a4 4 0 0 0 4 4',
  grid: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
  orders: 'M6 3h12v18l-3-2-3 2-3-2-3 2V3Z M9 7h6 M9 11h6 M9 15h3',
  chart: 'M4 3v18h17 M8 17v-5 M13 17V7 M18 17V4',
  users:
    'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M16 3a4 4 0 0 1 0 8 M22 21v-2a4 4 0 0 0-3-3.87 M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z',
  building: 'M3 21h18 M6 21V3h12v18 M9 7h1 M14 7h1 M9 11h1 M14 11h1 M10 21v-5h4v5',
  bell: 'M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9 M10 21h4',
  user: 'M20 21v-2a7 7 0 0 0-14 0v2 M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z',
  settings: 'M4 7h16 M4 17h16 M8 4v6 M16 14v6',
  logout: 'M9 21H3V3h6 M13 7l5 5-5 5 M8 12h12',
  arrow: 'M5 12h14 M13 6l6 6-6 6',
  plus: 'M12 5v14 M5 12h14',
  check: 'M5 12l4 4L19 6',
  truck:
    'M1 3h14v13H1z M15 8h4l3 4v4h-7 M8 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0Z M20 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0Z',
  clock: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z M12 6v6l4 2',
  menu: 'M3 6h18 M3 12h18 M3 18h18',
  shield: 'M12 2l9 4v6c0 5-9 10-9 10S3 17 3 12V6l9-4Z M8 12l3 3 5-6',
};
@Component({
  selector: 'app-icon',
  template:
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="path()" /></svg>',
  styles: [
    ':host{display:inline-flex;width:1.25em;height:1.25em;flex-shrink:0}svg{width:100%;height:100%}',
  ],
})
export class Icon {
  name = input('drop');
  path() {
    return paths[this.name()] ?? paths['drop'];
  }
}
