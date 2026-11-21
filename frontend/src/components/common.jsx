import { useEffect, useId, useRef } from 'react'

/** Muestra carga, error (con reintento) o vacío; si hay datos, renderiza children. */
export function AsyncState({ loading, error, onRetry, empty, emptyText = 'No hay datos para mostrar.', emptyAction, children }) {
  if (error) {
    return (
      <div className="banner banner-error" role="alert">
        <span>{error.message}</span>
        {onRetry && (
          <button className="btn btn-secondary btn-sm" onClick={onRetry}>
            Reintentar
          </button>
        )}
      </div>
    )
  }
  if (loading && empty !== false) {
    return (
      <p className="muted" role="status">
        Cargando…
      </p>
    )
  }
  if (empty) {
    return (
      <div className="empty">
        <p>{emptyText}</p>
        {emptyAction}
      </div>
    )
  }
  return children
}

export function KpiCard({ label, value, hint }) {
  return (
    <div className="card kpi">
      <span className="kpi-label">{label}</span>
      <span className="kpi-value">{value}</span>
      {hint && <span className="kpi-hint">{hint}</span>}
    </div>
  )
}

const BADGES = {
  ACTIVA: ['success', 'Activa'],
  ANULADA: ['neutral', 'Anulada'],
  BAJO_STOCK: ['warning', 'Bajo stock'],
  POR_VENCER: ['warning', 'Por vencer'],
  VENCIDO: ['danger', 'Vencido'],
  INACTIVO: ['neutral', 'Inactivo'],
}

export function StatusBadge({ tipo }) {
  const [tone, text] = BADGES[tipo] ?? ['neutral', tipo]
  return <span className={`badge badge-${tone}`}>{text}</span>
}

/** Diálogo modal accesible: foco inicial, Esc para cerrar, clic fuera para cerrar. */
export function Modal({ title, onClose, children, footer, wide }) {
  const ref = useRef(null)
  const titleId = useId()
  useEffect(() => {
    const prev = document.activeElement
    ref.current?.querySelector('input, select, button, textarea')?.focus()
    // Con modales anidados, Esc solo cierra el que está encima.
    const onKey = (e) => {
      const modales = document.querySelectorAll('.modal')
      if (e.key === 'Escape' && modales[modales.length - 1] === ref.current) onClose()
    }
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('keydown', onKey)
      prev?.focus?.()
    }
  }, [onClose])

  return (
    <div className="overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={`modal ${wide ? 'modal-wide' : ''}`} role="dialog" aria-modal="true" aria-labelledby={titleId} ref={ref}>
        <header className="modal-header">
          <h2 id={titleId}>{title}</h2>
          <button className="icon-btn" aria-label="Cerrar" onClick={onClose}>
            ×
          </button>
        </header>
        <div className="modal-body">{children}</div>
        {footer && <footer className="modal-footer">{footer}</footer>}
      </div>
    </div>
  )
}

export function ConfirmDialog({ title, message, confirmText = 'Confirmar', danger, busy, onConfirm, onCancel }) {
  return (
    <Modal
      title={title}
      onClose={onCancel}
      footer={
        <>
          <button className="btn btn-secondary" onClick={onCancel} disabled={busy}>
            Cancelar
          </button>
          <button className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`} onClick={onConfirm} disabled={busy}>
            {busy ? 'Procesando…' : confirmText}
          </button>
        </>
      }
    >
      <p>{message}</p>
    </Modal>
  )
}

/** Barras horizontales: [{label, value, display, sub}] */
export function BarList({ items, emptyText = 'Sin datos en el periodo.' }) {
  const max = Math.max(...items.map((i) => Number(i.value)), 0)
  if (!items.length || max === 0) return <p className="muted">{emptyText}</p>
  return (
    <ul className="barlist">
      {items.map((i) => (
        <li key={i.label}>
          <div className="barlist-row">
            <span>{i.label}</span>
            <span className="num">
              {i.display}
              {i.sub && <span className="muted"> · {i.sub}</span>}
            </span>
          </div>
          <div className="bar-track">
            <div className="bar-fill" style={{ width: `${(Number(i.value) / max) * 100}%` }} />
          </div>
        </li>
      ))}
    </ul>
  )
}

/** Columnas verticales por día. */
export function DayChart({ points, format, formatLabel }) {
  const max = Math.max(...points.map((p) => Number(p.total)), 0)
  if (!points.length) return <p className="muted">Sin datos.</p>
  const step = Math.ceil(points.length / 8)
  return (
    <div className="daychart" role="img" aria-label="Ventas por día">
      {points.map((p, i) => (
        <div className="daychart-col" key={p.fecha} title={`${formatLabel(p.fecha)}: ${format(p.total)}`}>
          <div className="daychart-bar-wrap">
            <div className="daychart-bar" style={{ height: max ? `${(Number(p.total) / max) * 100}%` : 0 }} />
          </div>
          <span className="daychart-label">{i % step === 0 ? formatLabel(p.fecha).slice(0, 5) : ''}</span>
        </div>
      ))}
    </div>
  )
}
