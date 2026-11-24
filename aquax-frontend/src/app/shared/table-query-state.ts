import { DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, ParamMap, Params, Router } from '@angular/router';
import {
  debounceTime,
  distinctUntilChanged,
  map,
  merge,
  Observable,
  shareReplay,
  Subject,
  tap,
} from 'rxjs';

export interface ListQuery {
  page: number;
  size: number;
  texto: string;
  sort: string;
}
export interface TableQueryOptions {
  defaultSort: string;
  allowedSortFields: string[];
  searchResetParams?: string[];
  searchFallbackParam?: string;
}

/** A list's URL is its source of truth: reload and browser history restore the same query. */
@Injectable()
export class TableQueryState {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly refreshed = new Subject<void>();
  private options?: TableQueryOptions;
  private current: ListQuery = { page: 0, size: 10, texto: '', sort: '' };
  readonly search = new FormControl('', { nonNullable: true });
  readonly page = signal(0);
  readonly size = signal(10);
  readonly sort = signal('');
  readonly params = signal<ParamMap>(this.route.snapshot.queryParamMap);

  private readonly routeQuery$ = this.route.queryParamMap.pipe(
    map((params) => {
      if (!this.options) throw new Error('Initialize TableQueryState before subscribing.');
      const query = parseListQuery(params, this.options);
      const extra = params.keys
        .filter((key) => !['page', 'size', 'texto', 'sort'].includes(key))
        .sort()
        .map((key) => [key, params.getAll(key)]);
      return { query, params, key: JSON.stringify([query, extra]) };
    }),
    distinctUntilChanged((previous, current) => previous.key === current.key),
    tap(({ query, params }) => {
      this.current = query;
      this.params.set(params);
      this.page.set(query.page);
      this.size.set(query.size);
      this.sort.set(query.sort);
      // A URL navigation replaces any uncommitted typing timer (including browser Back).
      // The subscriber below compares against current.texto, so this feedback never requests twice.
      this.search.setValue(query.texto);
    }),
    map(({ query }) => query),
    shareReplay({ bufferSize: 1, refCount: true }),
  );
  readonly changes$: Observable<ListQuery> = merge(
    this.routeQuery$,
    this.refreshed.pipe(map(() => this.current)),
  );

  initialize(options: TableQueryOptions) {
    if (this.options) throw new Error('TableQueryState was already initialized.');
    this.options = options;
    const query = parseListQuery(this.route.snapshot.queryParamMap, options);
    this.current = query;
    this.page.set(query.page);
    this.size.set(query.size);
    this.sort.set(query.sort);
    this.search.setValue(query.texto, { emitEvent: false });
    this.search.valueChanges
      .pipe(
        debounceTime(300),
        map((value) => value.trim()),
        distinctUntilChanged(
          (previous, current) => previous === current && current === this.current.texto,
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((texto) => {
        if (texto !== this.current.texto) this.navigate(this.searchChanges(texto), true);
      });
  }

  goTo(page: number) {
    this.navigate({ page: integer(page, 0) });
  }
  setSize(size: number) {
    this.navigate({ size: Math.min(50, Math.max(1, integer(size, 10))), page: 0 });
  }
  setSort(sort: string) {
    this.navigate({ sort: validSort(sort, this.options!), page: 0 });
  }
  searchNow() {
    const texto = this.search.value.trim();
    if (texto === this.current.texto && this.current.page === 0) this.refresh();
    else this.navigate(this.searchChanges(texto));
  }
  refresh() {
    this.refreshed.next();
  }

  private searchChanges(texto: string): Params {
    return {
      texto: texto || null,
      page: 0,
      ...Object.fromEntries((this.options?.searchResetParams ?? []).map((key) => [key, null])),
    };
  }

  private navigate(changes: Params, replaceUrl = false) {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        page: this.current.page,
        size: this.current.size,
        texto: this.current.texto || null,
        sort: this.current.sort,
        ...changes,
      },
      queryParamsHandling: 'merge',
      replaceUrl,
    });
  }
}

function integer(value: unknown, fallback: number) {
  if (!/^[0-9]+$/.test(String(value))) return fallback;
  const number = Number(value);
  return Number.isSafeInteger(number) ? number : fallback;
}
function validSort(value: string | null, options: TableQueryOptions) {
  const [field, direction, extra] = (value || options.defaultSort).split(',');
  return !extra && options.allowedSortFields.includes(field) && ['asc', 'desc'].includes(direction)
    ? `${field},${direction}`
    : options.defaultSort;
}
export function parseListQuery(params: ParamMap, options: TableQueryOptions): ListQuery {
  return {
    page: integer(params.get('page') ?? 0, 0),
    size: Math.min(50, Math.max(1, integer(params.get('size') ?? 10, 10))),
    texto: (
      params.get('texto') ??
      (options.searchFallbackParam ? params.get(options.searchFallbackParam) : null) ??
      ''
    ).trim(),
    sort: validSort(params.get('sort'), options),
  };
}
