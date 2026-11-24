import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Params, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { parseListQuery, TableQueryState } from './table-query-state';

const options = { defaultSort: 'nombre,asc', allowedSortFields: ['nombre', 'id'] };
function setup(initial: Params = {}) {
  let current = initial;
  const routeParams = new BehaviorSubject(convertToParamMap(current));
  const route = {
    queryParamMap: routeParams,
    get snapshot() {
      return { queryParamMap: convertToParamMap(current) };
    },
  };
  const navigate = vi.fn((_commands, settings) => {
    current = { ...current, ...settings.queryParams };
    for (const key of Object.keys(current)) if (current[key] === null) delete current[key];
    routeParams.next(convertToParamMap(current));
    return Promise.resolve(true);
  });
  TestBed.configureTestingModule({
    providers: [
      TableQueryState,
      { provide: ActivatedRoute, useValue: route },
      { provide: Router, useValue: { navigate } },
    ],
  });
  const state = TestBed.inject(TableQueryState);
  state.initialize(options);
  const queries: unknown[] = [];
  state.changes$.subscribe((query) => queries.push(query));
  return {
    state,
    queries,
    navigate,
    back: (params: Params) => {
      current = params;
      routeParams.next(convertToParamMap(params));
    },
  };
}

afterEach(() => vi.useRealTimers());
describe('TableQueryState URL contract', () => {
  it('restores reload and browser-history values without losing unrelated parameters', () => {
    const { state, navigate, back } = setup({
      page: '2',
      size: '25',
      texto: 'Pérez',
      sort: 'nombre,desc',
      vista: 'equipo',
    });
    expect(state.page()).toBe(2);
    expect(state.size()).toBe(25);
    expect(state.search.value).toBe('Pérez');
    state.goTo(3);
    expect(navigate.mock.lastCall?.[1]).toMatchObject({
      queryParamsHandling: 'merge',
      replaceUrl: false,
      queryParams: { page: 3, size: 25, texto: 'Pérez', sort: 'nombre,desc' },
    });
    back({ page: '1', size: '10', texto: 'Ana', sort: 'id,asc' });
    expect(state.page()).toBe(1);
    expect(state.sort()).toBe('id,asc');
    expect(state.search.value).toBe('Ana');
  });
  it('debounces typing for 300ms, resets page and replaces typed-history entries', async () => {
    vi.useFakeTimers();
    const { state, navigate, queries } = setup({ page: '4', texto: 'viejo' });
    state.search.setValue('a');
    await vi.advanceTimersByTimeAsync(150);
    state.search.setValue('agua');
    await vi.advanceTimersByTimeAsync(299);
    expect(navigate).not.toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(1);
    expect(navigate).toHaveBeenCalledTimes(1);
    expect(navigate.mock.lastCall?.[1]).toMatchObject({
      replaceUrl: true,
      queryParams: { page: 0, texto: 'agua' },
    });
    expect(queries).toHaveLength(2);
  });
  it('submits immediately and permits typing the same text after browser back', async () => {
    vi.useFakeTimers();
    const { state, navigate, back } = setup();
    state.search.setValue('ana');
    state.searchNow();
    expect(navigate).toHaveBeenCalledTimes(1);
    await vi.advanceTimersByTimeAsync(300);
    expect(navigate).toHaveBeenCalledTimes(1);
    back({ texto: 'otro' });
    state.search.setValue('ana');
    await vi.advanceTimersByTimeAsync(300);
    expect(navigate).toHaveBeenCalledTimes(2);
  });
  it('sanitizes noninteger values, limits size and rejects unlisted sort fields', () => {
    expect(
      parseListQuery(
        convertToParamMap({ page: '2.5', size: '999999', sort: 'contrasena,asc' }),
        options,
      ),
    ).toEqual({ page: 0, size: 50, texto: '', sort: 'nombre,asc' });
    expect(
      parseListQuery(convertToParamMap({ page: '9007199254740992', size: '0' }), options).page,
    ).toBe(0);
    expect(parseListQuery(convertToParamMap({ size: '0' }), options).size).toBe(1);
  });

  it.each(['-1', '1.5', '1e2', 'NaN', 'Infinity', ' 2 ', '9007199254740992'])(
    'rejects unsafe page input %s',
    (page) => {
      expect(parseListQuery(convertToParamMap({ page }), options).page).toBe(0);
    },
  );

  it.each(['nombre,sideways', 'nombre,asc,id', 'contrasena,desc', 'id', 'id,DESC'])(
    'uses the documented default for unsupported ordering %s',
    (sort) => {
      expect(parseListQuery(convertToParamMap({ sort }), options).sort).toBe('nombre,asc');
    },
  );
  it('cancels uncommitted typing when browser Back restores another URL', async () => {
    vi.useFakeTimers();
    const { state, navigate, back } = setup({ texto: 'actual' });
    state.search.setValue('texto todavía sin confirmar');
    await vi.advanceTimersByTimeAsync(100);
    back({ texto: 'restaurado', page: '2' });
    await vi.advanceTimersByTimeAsync(500);
    expect(navigate).not.toHaveBeenCalled();
    expect(state.search.value).toBe('restaurado');
    expect(state.page()).toBe(2);
  });
  it('refreshes unchanged criteria while repeated URL state does not request again', () => {
    const { state, queries, back } = setup({ texto: 'agua' });
    back({ texto: 'agua' });
    expect(queries).toHaveLength(1);
    state.refresh();
    expect(queries).toHaveLength(2);
    state.setSize(25);
    expect(state.page()).toBe(0);
    expect(state.size()).toBe(25);
  });
});
