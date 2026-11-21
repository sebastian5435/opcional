# Mockups (Google Stitch)

> ⚠️ **Pendiente del estudiante.** Google Stitch se usa con tu cuenta en <https://stitch.withgoogle.com/>, así que esta carpeta no se pudo generar automáticamente.
> Mientras tanto, las capturas del prototipo funcionando están en [../docs/capturas/](../docs/capturas/).

## Pasos

1. Abre Stitch, elige **Web** (no móvil) y un modo que equilibre velocidad y calidad.
2. Adjunta `../PRD.md` y `../design.md`.
3. Usa este prompt:

```
Con base en el PRD y el design.md adjuntos, diseña las vistas principales de una
aplicación web para la gestión de ventas de un supermercado de barrio (SuperVentas).
Incluye:
1) Tablero de ventas: 5 KPI (total vendido, número de ventas, ticket promedio,
   IVA, utilidad bruta), filtro de rango de fechas con atajos (Hoy, Mes actual,
   Últimos 30 días), 3 alertas (bajo stock, por vencer, vencidos), gráfico de
   barras por día, medios de pago, ventas por categoría y top 5 productos.
2) Punto de venta: lista de productos con buscador a la izquierda; carrito a la
   derecha con cajero, cantidades, medio de pago (Efectivo/Tarjeta/Transferencia),
   subtotal, IVA y total, y botón Confirmar venta.
3) Catálogo de productos: buscador, filtro por categoría, chips Todos/Bajo stock/
   Por vencer/Vencidos, tabla con badges de estado y formulario modal de producto.
4) Historial de ventas: filtros por fecha, medio de pago y estado; tabla; modal de
   detalle con botón Anular y diálogo de confirmación.
Paleta: verde #0F7B5F como primario, fondo #F6F7F9, ámbar para alertas y rojo para
vencido/anular. Tipografía Inter. Barra lateral fija de 220 px.
La interfaz debe ser profesional, clara, accesible y consistente. Incluye estados
vacíos, errores y confirmaciones. No añadas módulos fuera del alcance del PRD
(no proveedores, no facturación electrónica, no login).
```

4. Exporta el ZIP y descomprímelo aquí como `mockups/stitch/`, con los HTML y PNG.
5. Compara cada vista con `docs/capturas/` y anota las diferencias en el informe.
