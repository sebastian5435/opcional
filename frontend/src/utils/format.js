const cop = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
  minimumFractionDigits: 0,
})
const num = new Intl.NumberFormat('es-CO')

export const formatCOP = (v) => cop.format(Number(v ?? 0))
export const formatNum = (v) => num.format(Number(v ?? 0))

export function formatFecha(iso) {
  if (!iso) return '—'
  const [y, m, d] = iso.slice(0, 10).split('-')
  return `${d}/${m}/${y}`
}

export function formatFechaHora(iso) {
  if (!iso) return '—'
  return `${formatFecha(iso)} ${iso.slice(11, 16)}`
}

/** Fecha local (no UTC) en formato ISO yyyy-mm-dd. */
export function isoLocal(date = new Date()) {
  const p = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}`
}

export function sumarDias(iso, dias) {
  const [y, m, d] = iso.split('-').map(Number)
  return isoLocal(new Date(y, m - 1, d + dias))
}

export const METODOS_PAGO = {
  EFECTIVO: 'Efectivo',
  TARJETA: 'Tarjeta',
  TRANSFERENCIA: 'Transferencia',
}

/** Misma regla que el servidor (RN02): IVA por línea redondeado al peso. */
export function calcularLinea(precio, ivaPct, cantidad) {
  const subtotal = Math.round(Number(precio) * cantidad)
  const iva = Math.round((subtotal * ivaPct) / 100)
  return { subtotal, iva, total: subtotal + iva }
}
