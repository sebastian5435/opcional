import { useState } from 'react'

const VACIO = {
  codigoBarras: '',
  nombre: '',
  categoriaId: '',
  precioCompra: '',
  precioVenta: '',
  ivaPorcentaje: '19',
  stockActual: '',
  stockMinimo: '',
  fechaVencimiento: '',
}

function desdeProducto(p) {
  if (!p) return VACIO
  return {
    codigoBarras: p.codigoBarras,
    nombre: p.nombre,
    categoriaId: String(p.categoriaId),
    precioCompra: String(p.precioCompra),
    precioVenta: String(p.precioVenta),
    ivaPorcentaje: String(p.ivaPorcentaje),
    stockActual: String(p.stockActual),
    stockMinimo: String(p.stockMinimo),
    fechaVencimiento: p.fechaVencimiento ?? '',
  }
}

/** Validación en el cliente (RN03, RN12). El servidor valida de nuevo. */
export function validarProducto(v) {
  const e = {}
  const entero = (x) => /^\d+$/.test(String(x).trim())
  const monto = (x) => /^\d+(\.\d{1,2})?$/.test(String(x).trim())
  if (!v.codigoBarras.trim()) e.codigoBarras = 'es obligatorio'
  else if (!/^[0-9A-Za-z-]+$/.test(v.codigoBarras.trim())) e.codigoBarras = 'solo letras, números y guiones'
  if (!v.nombre.trim()) e.nombre = 'es obligatorio'
  if (!v.categoriaId) e.categoriaId = 'es obligatoria'
  if (v.precioCompra === '') e.precioCompra = 'es obligatorio'
  else if (!monto(v.precioCompra)) e.precioCompra = 'debe ser un número ≥ 0'
  if (v.precioVenta === '') e.precioVenta = 'es obligatorio'
  else if (!monto(v.precioVenta) || Number(v.precioVenta) <= 0) e.precioVenta = 'debe ser mayor que 0'
  else if (!e.precioCompra && Number(v.precioVenta) < Number(v.precioCompra))
    e.precioVenta = 'no puede ser menor que el precio de compra'
  if (!['0', '5', '19'].includes(String(v.ivaPorcentaje))) e.ivaPorcentaje = 'debe ser 0, 5 o 19'
  if (v.stockActual === '') e.stockActual = 'es obligatorio'
  else if (!entero(v.stockActual)) e.stockActual = 'debe ser un entero ≥ 0'
  if (v.stockMinimo === '') e.stockMinimo = 'es obligatorio'
  else if (!entero(v.stockMinimo)) e.stockMinimo = 'debe ser un entero ≥ 0'
  return e
}

export default function ProductoForm({ producto, categorias, onSubmit, onCancel }) {
  const [v, setV] = useState(() => desdeProducto(producto))
  const [errores, setErrores] = useState({})
  const [errorGeneral, setErrorGeneral] = useState(null)
  const [enviando, setEnviando] = useState(false)

  const set = (k) => (e) => setV((s) => ({ ...s, [k]: e.target.value }))

  async function submit(e) {
    e.preventDefault()
    const errs = validarProducto(v)
    setErrores(errs)
    setErrorGeneral(null)
    if (Object.keys(errs).length) return
    setEnviando(true)
    try {
      await onSubmit({
        codigoBarras: v.codigoBarras.trim(),
        nombre: v.nombre.trim(),
        categoriaId: Number(v.categoriaId),
        precioCompra: Number(v.precioCompra),
        precioVenta: Number(v.precioVenta),
        ivaPorcentaje: Number(v.ivaPorcentaje),
        stockActual: Number(v.stockActual),
        stockMinimo: Number(v.stockMinimo),
        fechaVencimiento: v.fechaVencimiento || null,
        activo: producto ? producto.activo : true,
      })
    } catch (err) {
      if (err.fields) setErrores(err.fields)
      setErrorGeneral(err.message)
      setEnviando(false)
    }
  }

  const campo = (k, label, input, requerido = true) => (
    <div className={`field ${errores[k] ? 'has-error' : ''}`}>
      <label htmlFor={`f-${k}`}>
        {label}
        {requerido && <span aria-hidden="true"> *</span>}
      </label>
      {input}
      {errores[k] && (
        <span className="field-error" id={`e-${k}`}>
          {label} {errores[k]}
        </span>
      )}
    </div>
  )
  const attrs = (k) => ({ id: `f-${k}`, value: v[k], onChange: set(k), 'aria-invalid': !!errores[k], 'aria-describedby': errores[k] ? `e-${k}` : undefined })

  return (
    <form onSubmit={submit} noValidate>
      {errorGeneral && (
        <div className="banner banner-error" role="alert">
          {errorGeneral}
        </div>
      )}
      <div className="form-grid">
        {campo('codigoBarras', 'Código de barras', <input {...attrs('codigoBarras')} maxLength={32} />)}
        {campo('nombre', 'Nombre', <input {...attrs('nombre')} maxLength={120} />)}
        {campo(
          'categoriaId',
          'Categoría',
          <select {...attrs('categoriaId')}>
            <option value="">Seleccione…</option>
            {categorias.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nombre}
              </option>
            ))}
          </select>,
        )}
        {campo(
          'ivaPorcentaje',
          'IVA',
          <select {...attrs('ivaPorcentaje')}>
            <option value="0">0 %</option>
            <option value="5">5 %</option>
            <option value="19">19 %</option>
          </select>,
        )}
        {campo('precioCompra', 'Precio de compra', <input {...attrs('precioCompra')} inputMode="decimal" />)}
        {campo('precioVenta', 'Precio de venta (sin IVA)', <input {...attrs('precioVenta')} inputMode="decimal" />)}
        {campo('stockActual', 'Stock actual', <input {...attrs('stockActual')} inputMode="numeric" />)}
        {campo('stockMinimo', 'Stock mínimo', <input {...attrs('stockMinimo')} inputMode="numeric" />)}
        {campo('fechaVencimiento', 'Fecha de vencimiento', <input type="date" {...attrs('fechaVencimiento')} />, false)}
      </div>
      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={enviando}>
          Cancelar
        </button>
        <button type="submit" className="btn btn-primary" disabled={enviando}>
          {enviando ? 'Guardando…' : 'Guardar'}
        </button>
      </div>
    </form>
  )
}
