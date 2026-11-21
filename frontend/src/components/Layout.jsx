import { NavLink, Outlet } from 'react-router-dom'

const LINKS = [
  { to: '/', label: 'Tablero', end: true },
  { to: '/pos', label: 'Punto de venta' },
  { to: '/productos', label: 'Productos' },
  { to: '/ventas', label: 'Ventas' },
]

export default function Layout() {
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            S
          </span>
          <span>SuperVentas</span>
        </div>
        <nav aria-label="Principal">
          {LINKS.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.end} className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}>
              {l.label}
            </NavLink>
          ))}
        </nav>
        <p className="sidebar-note">Prototipo · datos ficticios en H2 en memoria</p>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
