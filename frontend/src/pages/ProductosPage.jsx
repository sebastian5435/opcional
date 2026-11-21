import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { categoriasApi, productosApi } from '../api/services'
import ProductoForm from '../components/ProductoForm'
import { AsyncState, ConfirmDialog, Modal, StatusBadge } from '../components/common'
import { useAsync, useDebounced } from '../hooks/useAsync'
import { formatCOP, formatFecha, formatNum } from '../utils/format'

const FILTROS = [
  { id: '', label: 'Todos' },
  { id: 'BAJO_STOCK', label: 'Bajo stock' },
  { id: 'POR_VENCER', label: 'Por vencer' },
  { id: 'VENCIDOS', label: 'Vencidos' },
]

export default function ProductosPage() {
  const [params, setParams] = useSearchParams()
  const filtro = params.get('filtro') ?? ''
  const categoriaId = params.get('categoriaId') ?? ''
  const incluirInactivos = params.get('inactivos') === '1'
  const [q, setQ] = useState(params.get('q') ?? '')
  const qDeb = useDebounced(q)

  const [editando, setEditando] = useState(null) // null | 'nuevo' | producto
  const [aDesactivar, setADesactivar] = useState(null)
  const [desactivando, setDesactivando] = useState(false)
  const [aviso, setAviso] = useState(null)

  const categorias = useAsync(() => categoriasApi.listar(), [])
  const productos = useAsync(
    () => productosApi.listar({ q: qDeb, categoriaId, filtro, incluirInactivos }),
    [qDeb, categoriaId, filtro, incluirInactivos],
  )

  const setParam = (k, v) => {
    const p = new URLSearchParams(params)
    if (v) p.set(k, v)
    else p.delete(k)
    setParams(p, { replace: true })
  }

  async function guardar(data) {
    if (editando === 'nuevo') await productosApi.crear(data)
    else await productosApi.actualizar(editando.id, data)
    setAviso({ tipo: 'success', texto: `Producto “${data.nombre}” guardado.` })
    setEditando(null)
    productos.reload()
  }

  async function desactivar() {
    setDesactivando(true)
    try {
      await productosApi.desactivar(aDesactivar.id)
      setAviso({ tipo: 'success', texto: `Producto “${aDesactivar.nombre}” desactivado.` })
      productos.reload()
    } catch (e) {
      setAviso({ tipo: 'error', texto: e.message })
    } finally {
      setDesactivando(false)
      setADesactivar(null)
    }
  }

  const lista = productos.data ?? []

  return (
    <>
      <header className="page-header">
        <div>
          <h1>Catálogo de productos</h1>
          <p className="muted">Precios de venta sin IVA. Por vencer = vence en los próximos 7 días.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setEditando('nuevo')}>
          + Nuevo producto
        </button>
      </header>

      {aviso && (
        <div className={`banner banner-${aviso.tipo}`} role="status">
          <span>{aviso.texto}</span>
          <button className="icon-btn" aria-label="Cerrar aviso" onClick={() => setAviso(null)}>
            ×
          </button>
        </div>
      )}

      <div className="toolbar">
        <label className="inline-field grow">
          <span className="sr-only">Buscar</span>
          <input type="search" placeholder="Buscar por nombre o código…" value={q} onChange={(e) => {
            setQ(e.target.value)
            setParam('q', e.target.value)
          }} />
        </label>
        <label className="inline-field">
          Categoría
          <select value={categoriaId} onChange={(e) => setParam('categoriaId', e.target.value)}>
            <option value="">Todas</option>
            {(categorias.data ?? []).map((c) => (
              <option key={c.id} value={c.id}>
                {c.nombre}
              </option>
            ))}
          </select>
        </label>
        <label className="inline-field checkbox">
          <input type="checkbox" checked={incluirInactivos} onChange={(e) => setParam('inactivos', e.target.checked ? '1' : '')} />
          Incluir inactivos
        </label>
      </div>
      <div className="chip-group" role="group" aria-label="Filtro de estado">
        {FILTROS.map((f) => (
          <button key={f.id} className={`chip ${filtro === f.id ? 'chip-active' : ''}`} aria-pressed={filtro === f.id} onClick={() => setParam('filtro', f.id)}>
            {f.label}
          </button>
        ))}
        {!productos.loading && !productos.error && <span className="muted count">{lista.length} productos</span>}
      </div>

      <AsyncState
        loading={productos.loading}
        error={productos.error}
        onRetry={productos.reload}
        empty={lista.length === 0}
        emptyText="Ningún producto coincide con los filtros."
      >
        <div className={`card table-wrap ${productos.loading ? 'is-refreshing' : ''}`}>
          <table>
            <thead>
              <tr>
                <th>Código</th>
                <th>Producto</th>
                <th>Categoría</th>
                <th className="num">Compra</th>
                <th className="num">Venta</th>
                <th className="num">IVA</th>
                <th className="num">Stock</th>
                <th>Vence</th>
                <th>Estado</th>
                <th>
                  <span className="sr-only">Acciones</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {lista.map((p) => (
                <tr key={p.id} className={p.activo ? '' : 'row-muted'}>
                  <td className="mono">{p.codigoBarras}</td>
                  <td>{p.nombre}</td>
                  <td>{p.categoriaNombre}</td>
                  <td className="num">{formatCOP(p.precioCompra)}</td>
                  <td className="num">{formatCOP(p.precioVenta)}</td>
                  <td className="num">{p.ivaPorcentaje} %</td>
                  <td className="num">
                    {formatNum(p.stockActual)} <span className="muted">/ mín {p.stockMinimo}</span>
                  </td>
                  <td>{formatFecha(p.fechaVencimiento)}</td>
                  <td>
                    <div className="badges">
                      {!p.activo && <StatusBadge tipo="INACTIVO" />}
                      {p.bajoStock && <StatusBadge tipo="BAJO_STOCK" />}
                      {p.porVencer && <StatusBadge tipo="POR_VENCER" />}
                      {p.vencido && <StatusBadge tipo="VENCIDO" />}
                    </div>
                  </td>
                  <td className="actions">
                    <button className="btn btn-secondary btn-sm" onClick={() => setEditando(p)}>
                      Editar
                    </button>
                    {p.activo && (
                      <button className="btn btn-ghost-danger btn-sm" onClick={() => setADesactivar(p)}>
                        Desactivar
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>

      {editando && (
        <Modal title={editando === 'nuevo' ? 'Nuevo producto' : `Editar: ${editando.nombre}`} onClose={() => setEditando(null)} wide>
          <ProductoForm
            producto={editando === 'nuevo' ? null : editando}
            categorias={categorias.data ?? []}
            onSubmit={guardar}
            onCancel={() => setEditando(null)}
          />
        </Modal>
      )}

      {aDesactivar && (
        <ConfirmDialog
          title={`¿Desactivar “${aDesactivar.nombre}”?`}
          message="El producto dejará de aparecer en el punto de venta. Su historial de ventas se conserva."
          confirmText="Desactivar"
          danger
          busy={desactivando}
          onConfirm={desactivar}
          onCancel={() => setADesactivar(null)}
        />
      )}
    </>
  )
}
