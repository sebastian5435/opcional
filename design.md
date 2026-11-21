# design.md — Sistema visual de SuperVentas

Guía para mantener la consistencia entre los mockups de Google Stitch y el frontend React.

## Paleta

| Token | Claro | Uso |
|---|---|---|
| `--bg` | `#F6F7F9` | Fondo de la página |
| `--surface` | `#FFFFFF` | Tarjetas, tablas y modales |
| `--text` | `#1B2430` | Texto principal |
| `--muted` | `#5B6675` | Texto secundario y etiquetas |
| `--border` | `#E1E5EA` | Bordes y divisores |
| `--primary` | `#0F7B5F` | Acción principal, enlaces y barra activa (verde de mercado) |
| `--primary-ink` | `#FFFFFF` | Texto sobre el color primario |
| `--danger` | `#C0392B` | Errores, vencidos y anular |
| `--warning` | `#B26A00` | Bajo stock y por vencer |
| `--success` | `#1E8449` | Confirmaciones |

## Tipografía

- Familia: `Inter, system-ui, sans-serif`.
- Escala: 12 / 14 (cuerpo) / 16 / 20 / 28 (valor de KPI).
- Las cifras usan `font-variant-numeric: tabular-nums`.

## Espaciado y forma

- Base de 4 px; espacios habituales de 8, 12, 16 y 24 px.
- Radio de 8 px en tarjetas e inputs y de 6 px en botones.
- Sombra solo en modales.

## Componentes

- **Tarjeta KPI**: etiqueta (12 px, color muted), valor (28 px, semibold) y subtítulo opcional.
- **Botones**: primario (relleno `--primary`), secundario (borde), peligro (relleno `--danger`). Altura de 36 px y foco visible de 2 px.
- **Tabla**: cabecera fija en `--bg`, filas de 44 px, números alineados a la derecha y desplazamiento horizontal dentro del contenedor en el móvil.
- **Badge de estado**: ACTIVA en verde, ANULADA en gris, Bajo stock en ámbar, Por vencer en ámbar, Vencido en rojo.
- **Formulario**: etiqueta encima del campo, error en rojo debajo y asterisco en los campos obligatorios.
- **Estados**: carga (texto "Cargando…"), vacío (mensaje y acción sugerida) y error (banner rojo con botón Reintentar).
- **Confirmación**: modal con un título que nombra la acción ("¿Anular la venta V-00012?").

## Distribución

- Escritorio: barra lateral de 220 px y contenido con un máximo de 1200 px.
- Móvil (< 768 px): navegación superior horizontal y gutter de 16 px.
- Tablero: 5 KPI en una rejilla flexible, luego 2 columnas (tendencia + medios de pago, categorías + top productos).
