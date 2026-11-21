import { useState } from 'react'
import { Link } from 'react-router-dom'
import { dashboardApi } from '../api/services'
import { AsyncState, BarList, DayChart, KpiCard } from '../components/common'
import { useAsync } from '../hooks/useAsync'
import { METODOS_PAGO, formatCOP, formatFecha, formatNum, isoLocal, sumarDias } from '../utils/format'

function rangos() {
  const hoy = isoLocal()
  return {
    hoy: { desde: hoy, hasta: hoy, label: 'Hoy' },
    mes: { desde: hoy.slice(0, 8) + '01', hasta: hoy, label: 'Mes actual' },
    d30: { desde: sumarDias(hoy, -29), hasta: hoy, label: 'Últimos 30 días' },
  }
}

export default function DashboardPage() {
  const presets = rangos()
  const [rango, setRango] = useState({ desde: presets.mes.desde, hasta: presets.mes.hasta })
  const [borrador, setBorrador] = useState(rango)
  const rangoInvalido = borrador.desde > borrador.hasta

  const { data, loading, error, reload } = useAsync(() => dashboardApi.resumen(rango), [rango.desde, rango.hasta])

  const aplicar = (r) => {
    setBorrador(r)
    setRango(r)
  }

  return (
    <>
      <header className="page-header">
        <div>
          <h1>Tablero de ventas</h1>
          <p className="muted">
            {formatFecha(rango.desde)} – {formatFecha(rango.hasta)} · solo ventas activas
          </p>
        </div>
        <form
          className="filters"
          onSubmit={(e) => {
            e.preventDefault()
            if (!rangoInvalido) setRango(borrador)
          }}
        >
          <div className="chip-group" role="group" aria-label="Rangos rápidos">
            {Object.entries(presets).map(([k, p]) => (
              <button
                type="button"
                key={k}
                className={`chip ${rango.desde === p.desde && rango.hasta === p.hasta ? 'chip-active' : ''}`}
                onClick={() => aplicar({ desde: p.desde, hasta: p.hasta })}
              >
                {p.label}
              </button>
            ))}
          </div>
          <label className="inline-field">
            Desde
            <input type="date" value={borrador.desde} onChange={(e) => setBorrador((b) => ({ ...b, desde: e.target.value }))} required />
          </label>
          <label className="inline-field">
            Hasta
            <input type="date" value={borrador.hasta} onChange={(e) => setBorrador((b) => ({ ...b, hasta: e.target.value }))} required />
          </label>
          <button className="btn btn-secondary" disabled={rangoInvalido || !borrador.desde || !borrador.hasta}>
            Aplicar
          </button>
          {rangoInvalido && <span className="field-error">“Desde” no puede ser posterior a “Hasta”.</span>}
        </form>
      </header>

      <AsyncState loading={loading} error={error} onRetry={reload} empty={!data}>
        {data && (
          <div className={loading ? 'is-refreshing' : ''}>
            <section className="kpi-grid" aria-label="Indicadores">
              <KpiCard label="Total vendido" value={formatCOP(data.totalVentas)} hint="Incluye IVA" />
              <KpiCard label="Número de ventas" value={formatNum(data.numeroVentas)} />
              <KpiCard label="Ticket promedio" value={formatCOP(data.ticketPromedio)} />
              <KpiCard label="IVA generado" value={formatCOP(data.iva)} />
              <KpiCard label="Utilidad bruta" value={formatCOP(data.utilidadBruta)} hint="Ventas sin IVA − costo" />
            </section>

            <section className="alerts" aria-label="Alertas de inventario">
              <Link to="/productos?filtro=BAJO_STOCK" className="alert-card alert-warning">
                <strong>{data.alertas.bajoStock}</strong> con bajo stock
              </Link>
              <Link to="/productos?filtro=POR_VENCER" className="alert-card alert-warning">
                <strong>{data.alertas.porVencer}</strong> por vencer (7 días)
              </Link>
              <Link to="/productos?filtro=VENCIDOS" className="alert-card alert-danger">
                <strong>{data.alertas.vencidos}</strong> vencidos
              </Link>
            </section>

            {data.numeroVentas === 0 && (
              <div className="empty">
                <p>No hay ventas activas en este periodo.</p>
                <Link className="btn btn-primary" to="/pos">
                  Registrar una venta
                </Link>
              </div>
            )}

            <div className="grid-2">
              <section className="card">
                <h2>Ventas por día</h2>
                <DayChart points={data.ventasPorDia} format={formatCOP} formatLabel={formatFecha} />
              </section>
              <section className="card">
                <h2>Medios de pago</h2>
                <BarList
                  items={data.ventasPorMetodoPago.map((m) => ({
                    label: METODOS_PAGO[m.nombre],
                    value: m.total,
                    display: formatCOP(m.total),
                    sub: `${m.cantidad} ventas`,
                  }))}
                />
              </section>
              <section className="card">
                <h2>Ventas por categoría</h2>
                <BarList
                  items={data.ventasPorCategoria.map((c) => ({
                    label: c.nombre,
                    value: c.total,
                    display: formatCOP(c.total),
                    sub: `${formatNum(c.cantidad)} und.`,
                  }))}
                />
              </section>
              <section className="card">
                <h2>Productos más vendidos</h2>
                {data.topProductos.length === 0 ? (
                  <p className="muted">Sin ventas en el periodo.</p>
                ) : (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>#</th>
                          <th>Producto</th>
                          <th className="num">Unidades</th>
                          <th className="num">Total</th>
                        </tr>
                      </thead>
                      <tbody>
                        {data.topProductos.map((p, i) => (
                          <tr key={p.productoId}>
                            <td>{i + 1}</td>
                            <td>{p.nombre}</td>
                            <td className="num">{formatNum(p.unidades)}</td>
                            <td className="num">{formatCOP(p.total)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            </div>
          </div>
        )}
      </AsyncState>
    </>
  )
}
