/** Error de la API con el código HTTP, el mensaje del servidor y los errores por campo. */
export class ApiError extends Error {
  constructor(status, message, fields = null) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

const BASE = '/api'

export async function request(path, { method = 'GET', body, params } = {}) {
  let url = BASE + path
  if (params) {
    const qs = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.append(k, v)
    })
    const s = qs.toString()
    if (s) url += '?' + s
  }

  let res
  try {
    res = await fetch(url, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.')
  }

  if (res.status === 204) return null
  const data = await res.json().catch(() => null)
  // Un 5xx sin cuerpo JSON viene del proxy de desarrollo: el backend no está disponible.
  if (!res.ok && res.status >= 500 && !data) {
    throw new ApiError(res.status, 'No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.')
  }
  if (!res.ok) {
    const msg =
      data?.message ||
      (res.status >= 500 ? 'Error interno del servidor.' : `La petición falló (HTTP ${res.status}).`)
    throw new ApiError(res.status, msg, data?.fields ?? null)
  }
  return data
}
