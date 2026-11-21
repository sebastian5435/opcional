import { Link, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import DashboardPage from './pages/DashboardPage'
import PosPage from './pages/PosPage'
import ProductosPage from './pages/ProductosPage'
import VentasPage from './pages/VentasPage'

function NoEncontrada() {
  return (
    <div className="empty">
      <p>La página no existe.</p>
      <Link className="btn btn-primary" to="/">
        Ir al tablero
      </Link>
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<DashboardPage />} />
        <Route path="pos" element={<PosPage />} />
        <Route path="productos" element={<ProductosPage />} />
        <Route path="ventas" element={<VentasPage />} />
        <Route path="*" element={<NoEncontrada />} />
      </Route>
    </Routes>
  )
}
