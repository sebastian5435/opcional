import { request } from './client'

export const categoriasApi = {
  listar: () => request('/categorias'),
}

export const productosApi = {
  listar: (params) => request('/productos', { params }),
  obtener: (id) => request(`/productos/${id}`),
  crear: (data) => request('/productos', { method: 'POST', body: data }),
  actualizar: (id, data) => request(`/productos/${id}`, { method: 'PUT', body: data }),
  desactivar: (id) => request(`/productos/${id}`, { method: 'DELETE' }),
}

export const ventasApi = {
  listar: (params) => request('/ventas', { params }),
  obtener: (id) => request(`/ventas/${id}`),
  registrar: (data) => request('/ventas', { method: 'POST', body: data }),
  anular: (id) => request(`/ventas/${id}/anular`, { method: 'POST' }),
}

export const dashboardApi = {
  resumen: (params) => request('/dashboard', { params }),
}
