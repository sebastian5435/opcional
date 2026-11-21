import { useCallback, useEffect, useRef, useState } from 'react'

/**
 * Ejecuta una función asíncrona y expone {data, loading, error, reload}.
 * Ignora respuestas de peticiones anteriores (evita condiciones de carrera al filtrar).
 */
export function useAsync(fn, deps) {
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const seq = useRef(0)

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const run = useCallback(fn, deps)

  const reload = useCallback(() => {
    const id = ++seq.current
    setState((s) => ({ ...s, loading: true, error: null }))
    run()
      .then((data) => id === seq.current && setState({ data, loading: false, error: null }))
      .catch((error) => id === seq.current && setState((s) => ({ ...s, loading: false, error })))
  }, [run])

  useEffect(() => {
    reload()
  }, [reload])

  return { ...state, reload }
}

export function useDebounced(value, ms = 300) {
  const [v, setV] = useState(value)
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms)
    return () => clearTimeout(t)
  }, [value, ms])
  return v
}
