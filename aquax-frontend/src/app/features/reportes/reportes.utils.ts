export function rangoValido(desde: string, hasta: string): boolean {
  const fechaValida = (value: string) => {
    if (!value) return true;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
    const parsed = new Date(`${value}T00:00:00Z`);
    return Number.isFinite(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
  };
  return fechaValida(desde) && fechaValida(hasta) && (!desde || !hasta || desde <= hasta);
}

export function nombreCsv(contentDisposition: string | null): string {
  const fallback = 'aquax-historial.csv';
  if (!contentDisposition) return fallback;
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
  const plain = /filename\s*=\s*(?:"([^"]*)"|([^;]+))/i.exec(contentDisposition);
  let name = encoded?.[1] || plain?.[1] || plain?.[2] || '';
  try {
    if (encoded) name = decodeURIComponent(name);
  } catch {
    return fallback;
  }
  name = (name.split(/[\\/]/).pop() || '').trim().replace(/[\x00-\x1f<>:"|?*]/g, '');
  return name && /\.csv$/i.test(name) ? name : fallback;
}
