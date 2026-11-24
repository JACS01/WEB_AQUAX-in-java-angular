/** Labels stay in the UI; only the backend's allowed field names travel in the URL. */
export function adminSortOptions(
  fields: [string, string][],
  ascending = 'ascendente',
  descending = 'descendente',
) {
  return fields.flatMap(([field, label]) => [
    { value: `${field},asc`, label: `${label} · ${ascending}` },
    { value: `${field},desc`, label: `${label} · ${descending}` },
  ]);
}
