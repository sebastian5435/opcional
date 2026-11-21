import { useMemo, useState } from 'react'
import { productosApi, ventasApi } from '../api/services'
import { AsyncState } from '../components/common'
import { useAsync, useDebounced } from '../hooks/useAsync'
import { METODOS_PAGO, calcularLinea, formatCOP } from '../utils/format'

function leerCajero() {
  try {
    return localStorage.getItem('superventas.cajero') ?? ''
  } catch {
    return ''
  }
}

export default function PosPage() {
  const [q, setQ] = useState('')
  const qDeb = useDebounced(q, 200)
  const [cajero, setCajero] = useState(leerCajero)
  const [metodoPago, setMetodoPago] = useState('EFECTIVO')
  const [carrito, setCarrito] = useState([]) // [{producto, cantidad}]
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState(null)
  const [exito, setExito] = useState(null)
  const [cajeroError, setCajeroError] = useState(null)

  const productos = useAsync(() => productosApi.listar({ q: qDeb }), [qDeb])
  const disponibles = (productos.data ?? []).filter((p) => p.stockActual > 0 && !p.vencido)

  const totales = useMemo(() => {
    return carrito.reduce(
      (acc, { producto, cantidad }) => {
        const l = calcularLinea(producto.precioVenta, producto.ivaPorcentaje, cantidad)
        return { subtotal: acc.subtotal + l.subtotal, iva: acc.iva + l.iva, total: acc.total + l.total }
      },
      { subtotal: 0, iva: 0, total: 0 },
    )
  }, [carrito])

  function agregar(p) {
    setExito(null)
    setError(null)
    setCarrito((c) => {
      const i = c.findIndex((x) => x.producto.id === p.id)
      if (i >= 0) {
        if (c[i].cantidad >= p.stockActual) return c
        return c.map((x, j) => (j === i ? { ...x, cantidad: x.cantidad + 1 } : x))
      }
      return [...c, { producto: p, cantidad: 1 }]
    })
  }

  function cambiarCantidad(id, valor) {
    const n = Math.floor(Number(valor))
    setCarrito((c) =>
      c.map((x) => (x.producto.id === id ? { ...x, cantidad: Number.isFinite(n) ? Math.max(1, Math.min(n, x.producto.stockActual)) : 1 } : x)),
    )
  }

  const quitar = (id) => setCarrito((c) => c.filter((x) => x.producto.id !== id))

  async function confirmar() {
    setError(null)
    if (!cajero.trim()) {
      setCajeroError('Escriba el nombre del cajero')
      return
    }
    setCajeroError(null)
    setEnviando(true)
    try {
      const venta = await ventasApi.registrar({
        cajero: cajero.trim(),
        metodoPago,
        items: carrito.map((x) => ({ productoId: x.producto.id, cantidad: x.cantidad })),
      })
      try {
        localStorage.setItem('superventas.cajero', cajero.trim())
      } catch {
        /* almacenamiento no disponible */
      }
      setExito(venta)
      setCarrito([])
      productos.reload()
    } catch (e) {
      setError(e.message)
      productos.reload() // refresca el stock para que el cajero vea el disponible real
    } finally {
      setEnviando(false)
    }
  }

  return (
    <>
      <header className="page-header">
        <div>
          <h1>Punto de venta</h1>
          <p className="muted">Precios sin IVA; el IVA se suma por línea.</p>
        </div>
      </header>

      {exito && (
        <div className="banner banner-success" role="status">
          <span>
            Venta <strong>{exito.numero}</strong> registrada por {formatCOP(exito.total)}.
          </span>
          <button className="icon-btn" aria-label="Cerrar aviso" onClick={() => setExito(null)}>
            ×
          </button>
        </div>
      )}
      {error && (
        <div className="banner banner-error" role="alert">
          <span>{error}</span>
          <button className="icon-btn" aria-label="Cerrar aviso" onClick={() => setError(null)}>
            ×
          </button>
        </div>
      )}

      <div className="pos">
        <section className="card pos-catalog">
          <h2>Productos</h2>
          <label className="inline-field grow">
            <span className="sr-only">Buscar producto</span>
            <input type="search" placeholder="Buscar por nombre o código…" value={q} onChange={(e) => setQ(e.target.value)} autoFocus />
          </label>
          <AsyncState
            loading={productos.loading}
            error={productos.error}
            onRetry={productos.reload}
            empty={disponibles.length === 0}
            emptyText="No hay productos disponibles con ese criterio."
          >
            <ul className="pos-list">
              {disponibles.map((p) => {
                const enCarrito = carrito.find((x) => x.producto.id === p.id)?.cantidad ?? 0
                const agotado = enCarrito >= p.stockActual
                return (
                  <li key={p.id}>
                    <button className="pos-item" onClick={() => agregar(p)} disabled={agotado} aria-label={`Agregar ${p.nombre}`}>
                      <span className="pos-item-name">{p.nombre}</span>
                      <span className="muted mono">{p.codigoBarras}</span>
                      <span className="pos-item-price">{formatCOP(p.precioVenta)}</span>
                      <span className={`muted ${p.bajoStock ? 'text-warning' : ''}`}>
                        Stock {p.stockActual - enCarrito} · IVA {p.ivaPorcentaje} %
                      </span>
                    </button>
                  </li>
                )
              })}
            </ul>
          </AsyncState>
        </section>

        <section className="card pos-cart" aria-label="Carrito">
          <h2>Venta actual</h2>
          <div className={`field ${cajeroError ? 'has-error' : ''}`}>
            <label htmlFor="cajero">
              Cajero <span aria-hidden="true">*</span>
            </label>
            <input id="cajero" value={cajero} maxLength={80} onChange={(e) => setCajero(e.target.value)} aria-invalid={!!cajeroError} />
            {cajeroError && <span className="field-error">{cajeroError}</span>}
          </div>

          {carrito.length === 0 ? (
            <p className="empty">Agregue productos desde la lista.</p>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th className="num">Cant.</th>
                    <th className="num">Total</th>
                    <th>
                      <span className="sr-only">Quitar</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {carrito.map(({ producto, cantidad }) => (
                    <tr key={producto.id}>
                      <td>
                        {producto.nombre}
                        <div className="muted small">
                          {formatCOP(producto.precioVenta)} + IVA {producto.ivaPorcentaje} %
                        </div>
                      </td>
                      <td className="num">
                        <input
                          className="qty"
                          type="number"
                          min="1"
                          max={producto.stockActual}
                          value={cantidad}
                          aria-label={`Cantidad de ${producto.nombre}`}
                          onChange={(e) => cambiarCantidad(producto.id, e.target.value)}
                        />
                      </td>
                      <td className="num">{formatCOP(calcularLinea(producto.precioVenta, producto.ivaPorcentaje, cantidad).total)}</td>
                      <td>
                        <button className="icon-btn" aria-label={`Quitar ${producto.nombre}`} onClick={() => quitar(producto.id)}>
                          ×
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <fieldset className="pay">
            <legend>Medio de pago</legend>
            {Object.entries(METODOS_PAGO).map(([k, label]) => (
              <label key={k} className={`chip ${metodoPago === k ? 'chip-active' : ''}`}>
                <input type="radio" name="metodo" value={k} checked={metodoPago === k} onChange={() => setMetodoPago(k)} />
                {label}
              </label>
            ))}
          </fieldset>

          <dl className="totals">
            <dt>Subtotal</dt>
            <dd>{formatCOP(totales.subtotal)}</dd>
            <dt>IVA</dt>
            <dd>{formatCOP(totales.iva)}</dd>
            <dt className="grand">Total</dt>
            <dd className="grand">{formatCOP(totales.total)}</dd>
          </dl>

          <div className="form-actions">
            <button className="btn btn-secondary" onClick={() => setCarrito([])} disabled={!carrito.length || enviando}>
              Vaciar
            </button>
            <button className="btn btn-primary btn-lg" onClick={confirmar} disabled={!carrito.length || enviando}>
              {enviando ? 'Registrando…' : 'Confirmar venta'}
            </button>
          </div>
        </section>
      </div>
    </>
  )
}
