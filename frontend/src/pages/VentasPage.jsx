import { useState } from 'react'
import { ventasApi } from '../api/services'
import { AsyncState, ConfirmDialog, Modal, StatusBadge } from '../components/common'
import { useAsync } from '../hooks/useAsync'
import { METODOS_PAGO, formatCOP, formatFecha, formatFechaHora, formatNum, isoLocal, sumarDias } from '../utils/format'

function DetalleVenta({ id, onClose, onAnulada }) {
  const { data: v, loading, error, reload } = useAsync(() => ventasApi.obtener(id), [id])
  const [confirmar, setConfirmar] = useState(false)
  const [anulando, setAnulando] = useState(false)
  const [errorAnular, setErrorAnular] = useState(null)

  async function anular() {
    setAnulando(true)
    setErrorAnular(null)
    try {
      await ventasApi.anular(id)
      setConfirmar(false)
      reload()
      onAnulada()
    } catch (e) {
      setErrorAnular(e.message)
      setConfirmar(false)
    } finally {
      setAnulando(false)
    }
  }

  return (
    <>
      <Modal
        title={v ? `Venta ${v.numero}` : 'Detalle de venta'}
        onClose={onClose}
        wide
        footer={
          v?.estado === 'ACTIVA' && (
            <button className="btn btn-danger" onClick={() => setConfirmar(true)}>
              Anular venta
            </button>
          )
        }
      >
        {errorAnular && (
          <div className="banner banner-error" role="alert">
            {errorAnular}
          </div>
        )}
        <AsyncState loading={loading} error={error} onRetry={reload} empty={!v}>
          {v && (
            <>
              <dl className="meta">
                <div>
                  <dt>Fecha</dt>
                  <dd>{formatFechaHora(v.fechaHora)}</dd>
                </div>
                <div>
                  <dt>Cajero</dt>
                  <dd>{v.cajero}</dd>
                </div>
                <div>
                  <dt>Medio de pago</dt>
                  <dd>{METODOS_PAGO[v.metodoPago]}</dd>
                </div>
                <div>
                  <dt>Estado</dt>
                  <dd>
                    <StatusBadge tipo={v.estado} />
                  </dd>
                </div>
              </dl>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Producto</th>
                      <th className="num">Cant.</th>
                      <th className="num">Precio</th>
                      <th className="num">IVA</th>
                      <th className="num">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {v.detalles.map((d) => (
                      <tr key={d.id}>
                        <td>{d.productoNombre}</td>
                        <td className="num">{d.cantidad}</td>
                        <td className="num">{formatCOP(d.precioUnitario)}</td>
                        <td className="num">
                          {formatCOP(d.iva)} <span className="muted">({d.ivaPorcentaje} %)</span>
                        </td>
                        <td className="num">{formatCOP(d.total)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <dl className="totals">
                <dt>Subtotal</dt>
                <dd>{formatCOP(v.subtotal)}</dd>
                <dt>IVA</dt>
                <dd>{formatCOP(v.iva)}</dd>
                <dt className="grand">Total</dt>
                <dd className="grand">{formatCOP(v.total)}</dd>
              </dl>
            </>
          )}
        </AsyncState>
      </Modal>
      {confirmar && v && (
        <ConfirmDialog
          title={`¿Anular la venta ${v.numero}?`}
          message={`Se restaurará el stock de ${v.detalles.length} producto(s) y la venta dejará de contar en los indicadores. Esta acción no se puede deshacer.`}
          confirmText="Anular"
          danger
          busy={anulando}
          onConfirm={anular}
          onCancel={() => setConfirmar(false)}
        />
      )}
    </>
  )
}

export default function VentasPage() {
  const hoy = isoLocal()
  const [f, setF] = useState({ desde: sumarDias(hoy, -6), hasta: hoy, metodoPago: '', estado: '' })
  const [seleccion, setSeleccion] = useState(null)
  const invalido = f.desde && f.hasta && f.desde > f.hasta

  const ventas = useAsync(
    () => (invalido ? Promise.resolve([]) : ventasApi.listar(f)),
    [f.desde, f.hasta, f.metodoPago, f.estado, invalido],
  )
  const set = (k) => (e) => setF((s) => ({ ...s, [k]: e.target.value }))
  const lista = ventas.data ?? []
  const totalActivas = lista.filter((v) => v.estado === 'ACTIVA').reduce((a, v) => a + Number(v.total), 0)

  return (
    <>
      <header className="page-header">
        <div>
          <h1>Historial de ventas</h1>
          <p className="muted">
            {formatFecha(f.desde)} – {formatFecha(f.hasta)}
          </p>
        </div>
      </header>

      <div className="toolbar">
        <label className="inline-field">
          Desde
          <input type="date" value={f.desde} onChange={set('desde')} />
        </label>
        <label className="inline-field">
          Hasta
          <input type="date" value={f.hasta} onChange={set('hasta')} />
        </label>
        <label className="inline-field">
          Medio de pago
          <select value={f.metodoPago} onChange={set('metodoPago')}>
            <option value="">Todos</option>
            {Object.entries(METODOS_PAGO).map(([k, l]) => (
              <option key={k} value={k}>
                {l}
              </option>
            ))}
          </select>
        </label>
        <label className="inline-field">
          Estado
          <select value={f.estado} onChange={set('estado')}>
            <option value="">Todos</option>
            <option value="ACTIVA">Activa</option>
            <option value="ANULADA">Anulada</option>
          </select>
        </label>
      </div>
      {invalido && <p className="field-error">“Desde” no puede ser posterior a “Hasta”.</p>}

      <AsyncState
        loading={ventas.loading}
        error={ventas.error}
        onRetry={ventas.reload}
        empty={lista.length === 0}
        emptyText="No hay ventas con estos filtros."
      >
        <p className="muted count">
          {lista.length} ventas · total activas {formatCOP(totalActivas)}
        </p>
        <div className={`card table-wrap ${ventas.loading ? 'is-refreshing' : ''}`}>
          <table className="clickable">
            <thead>
              <tr>
                <th>Número</th>
                <th>Fecha</th>
                <th>Cajero</th>
                <th>Medio de pago</th>
                <th className="num">Unidades</th>
                <th className="num">Total</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((v) => (
                <tr key={v.id} className={v.estado === 'ANULADA' ? 'row-muted' : ''}>
                  <td>
                    <button className="link-btn" onClick={() => setSeleccion(v.id)}>
                      {v.numero}
                    </button>
                  </td>
                  <td>{formatFechaHora(v.fechaHora)}</td>
                  <td>{v.cajero}</td>
                  <td>{METODOS_PAGO[v.metodoPago]}</td>
                  <td className="num">{formatNum(v.items)}</td>
                  <td className="num">{formatCOP(v.total)}</td>
                  <td>
                    <StatusBadge tipo={v.estado} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>

      {seleccion && <DetalleVenta id={seleccion} onClose={() => setSeleccion(null)} onAnulada={ventas.reload} />}
    </>
  )
}
