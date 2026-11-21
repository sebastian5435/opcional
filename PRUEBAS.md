# Matriz de pruebas — SuperVentas

- **Fecha de ejecución:** 2026-11-21
- **Entorno:** Windows 11, JDK 26.0.2, Node 24.21.0, backend en `http://localhost:8080` y frontend Vite en `http://localhost:5173`, navegador Chromium (panel del IDE) a ~1009 px y 375 px.
- **Datos:** semilla automática (20 productos, 64 ventas de los últimos 30 días, 2 de ellas anuladas).

Todos los resultados de este documento se **ejecutaron de verdad**. Las capturas están en [docs/capturas/](docs/capturas/).

## 1. Pruebas automatizadas del backend

Comando: `cd backend && mvnw.cmd test` → **11 pruebas, 0 fallos, 0 errores** (`target/surefire-reports`).

| Prueba (`ApiIntegrationTest`, con reloj fijo en 2026-03-15) | Regla | Resultado |
|---|---|---|
| `registrarVentaCalculaIvaTotalesYDescuentaStock`: 3 × 1.000 (IVA 19 %) + 2 × 2.500 (IVA 0 %) → subtotal 8.000, IVA 570, total 8.570; el stock baja de 10 a 7 y de 5 a 3 | RN02, RN04, RN06 | ✅ |
| `ventaSinStockDevuelve409YNoModificaNada`: una línea válida + una sin stock → 409, ningún stock cambia y no se crea la venta | RN05 | ✅ |
| `lineasRepetidasSeSumanAlValidarStock`: 3 + 3 del mismo producto con stock 5 → 409 | RN05 | ✅ |
| `ventaSinItemsOConCantidadInvalidaDevuelve400`: lista vacía o cantidad 0 → 400 con `fields` | RNF06 | ✅ |
| `anularRestauraStockYNoPermiteAnularDosVeces` | RN07 | ✅ |
| `dashboardExcluyeVentasAnuladasYCalculaIndicadores`: rango por defecto 01–15 de marzo, 2 ventas, total 6.380, IVA 380, utilidad 1.800, ticket 3.190, 15 días y alertas 1/1/1 | RN07, RN10, RN11, RN14 | ✅ |
| `filtrosDeProductosCumplenLasReglas`: bajo stock, por vencer, vencidos, búsqueda sin distinguir mayúsculas y filtro inválido → 400 | RN08, RN09 | ✅ |
| `crearProductoValidaReglasYCodigoUnico`: IVA 7 % y venta < compra → 400; código duplicado → 409; campos vacíos → 400 | RN03, RN12 | ✅ |
| `desactivarProductoImpideVenderlo` | RN05, RN13 | ✅ |
| `recursosInexistentesDevuelven404SinTraza` | RNF06 | ✅ |
| `SeedDataTest.cargaDatosDePrueba`: 20 productos, ≥ 30 ventas, alguna anulada y ningún stock negativo | RNF07 | ✅ |

## 2. Pruebas manuales de la API (`Invoke-RestMethod` / `curl`)

| Petición | Resultado observado |
|---|---|
| `GET /api/productos` | 200, 20 productos, **119 ms** |
| `GET /api/productos?filtro=BAJO_STOCK` | Café (8/10), Cerveza (12/24), Crema dental (4/6) |
| `GET /api/productos?filtro=POR_VENCER` | Banano, Leche, Pan, Queso y Tomate (vencen entre el 24 y el 28 de noviembre) |
| `GET /api/productos?filtro=VENCIDOS` | Yogurt (vence el 19 de noviembre) |
| `GET /api/dashboard` | 200 en **93 ms**: total $1.232.219, 42 ventas, ticket $29.339, IVA $145.219, utilidad $273.700. Suma por medio de pago = suma por categoría = total ✔ |
| `POST /api/ventas` (2 arroz + 3 gaseosa) | 201, V-00065, subtotal 18.500, IVA 2.565, total 21.065 ✔; stock del arroz 133 → 131 |
| `POST /api/ventas` (500 arroz) | 409 `Stock insuficiente para 'Arroz Diana 500 g': disponible 133, solicitado 500` |
| `POST /api/ventas/{id}/anular` ×2 | 200 ANULADA (stock vuelve a 133); la segunda vez → 409 |
| `POST /api/productos` con un cuerpo incompleto | 400 con `fields` para los 8 campos |
| `GET /api/productos?filtro=XYZ` | 400 `Valor inválido para el parámetro 'filtro'` |
| `POST /api/ventas` con JSON mal formado | 400, mensaje genérico, sin traza |
| `GET /api/noexiste` | 404 en JSON, sin traza |

## 3. Matriz funcional de la guía (P01–P10)

| ID | Prueba | Resultado esperado | Resultado obtenido | Estado | Evidencia |
|---|---|---|---|---|---|
| P01 | Abrir el tablero | Indicadores coherentes con la API | Los KPI muestran exactamente los valores de `GET /api/dashboard` (1.232.219 / 42 / 29.339 / 145.219 / 273.700), con alertas 3/5/1, gráfico de 21 días, medios de pago, categorías y top 5 | ✅ Aprobada | 01, 02 |
| P02 | Consultar el catálogo | La tabla muestra registros del backend | 20 productos con código, categoría, precios, IVA, stock/mínimo, vencimiento y estado | ✅ Aprobada | 05 |
| P03 | Filtrar productos críticos | Cumplen la condición documentada (RN08) | "Bajo stock" muestra 3 productos, todos con stock ≤ mínimo | ✅ Aprobada | 05 |
| P04 | Filtrar por vencimiento | Solo los que cumplen el criterio (RN09) | "Por vencer" muestra 5 (≤ 7 días) y "Vencidos" muestra 1 | ✅ Aprobada | 13 (antes de corregir F03) |
| P05 | Consultar el historial de ventas *(adaptación de "proveedores")* | Listado con datos relevantes | La tabla filtra por fechas, medio de pago y estado, con el total de las ventas activas | ✅ Aprobada | 08 |
| P06 | Registrar una venta *(adaptación de "sugerencia de compra")* | Cálculo real y descuento de stock | El carrito calcula $28.855, la API confirma V-00065 por $28.855 y el stock se actualiza en la lista | ✅ Aprobada | 03, 04 |
| P07 | Datos vacíos o error de la API | Mensaje útil y la interfaz no se bloquea | Búsqueda sin resultados → "Ningún producto coincide…". Backend apagado → banner con "Reintentar" (ver el fallo F02, corregido) | ✅ Aprobada tras corregir | 11, 12 |
| P08 | Validar formularios | Campos obligatorios e inválidos gestionados | Cliente: mensaje por campo con el formulario vacío y "venta < compra". Servidor: el código duplicado muestra el 409 en el formulario. POS: cajero obligatorio | ✅ Aprobada | 03, 06, 07 |
| P09 | Recargar y reiniciar | Comportamiento de H2 documentado | Al recargar el navegador los datos se mantienen. Al **reiniciar el backend**, la venta de prueba V-00065 desaparece y los datos vuelven a la semilla (mismos totales gracias a la semilla fija) | ✅ Aprobada (comportamiento esperado) | — |
| P10 | Diseño adaptable | Legible en distintos tamaños | A 375 px el `scrollWidth` es igual al ancho de la ventana en las 4 vistas, la navegación pasa a ser superior y las tablas se desplazan dentro de su contenedor. A ~1009 px se ve bien (tras corregir F01) | ✅ Aprobada tras corregir | 10 |
| P11 | Anular una venta | Se pide confirmación, el stock se restaura y los indicadores se recalculan | V-00065 pasa a ANULADA, el total de activas baja 28.855, el stock del arroz vuelve a 133 y el tablero vuelve a $1.232.219 | ✅ Aprobada | 09 |

> **Adaptación al caso:** la guía original (inventario) incluye proveedores y sugerencias de compra. En este caso de estudio (supermercado) esas pruebas se reemplazan por el historial de ventas (P05) y el registro de ventas (P06). Proveedores y compras quedaron **fuera del alcance** del PRD.

## 4. Registro de fallos

| ID | Pasos para reproducir | Resultado actual | Resultado esperado | Evidencia | Hipótesis | Corrección aplicada |
|---|---|---|---|---|---|---|
| F01 | Abrir `/productos` a ~1000 px | La **página** entera tenía scroll horizontal (scrollWidth 1030 > 1009) | Solo se desplaza la tabla, dentro de su contenedor | JS: `documentElement.scrollWidth` | El `<span class="sr-only">` absoluto de la cabecera "Acciones" escapaba del contenedor `overflow:auto` porque este no estaba posicionado | `.table-wrap { position: relative }`. Verificado: scrollWidth = 1009 ✅ |
| F02 | Apagar el backend y abrir el tablero | Banner "Error interno del servidor." | Mensaje que explique que no hay conexión con el backend | 12 | El proxy de Vite responde 500 sin cuerpo JSON cuando no puede conectar | `client.js`: un 5xx sin JSON se trata como error de conexión. Verificado: "No se pudo conectar con el servidor…" ✅ |
| F03 | Abrir `/productos?filtro=POR_VENCER` | La columna Estado se descuadraba (los bordes de las celdas no coincidían) | Badges alineados en la celda | 13 | `display:flex` aplicado directamente a un `<td>` rompe el modelo de tabla | Los badges se envuelven en un `<div class="badges">` ✅ |
| F04 | Abrir el detalle de una venta y luego "Anular" | Dos `id="modal-title"` duplicados, y Esc cerraba los dos modales | IDs únicos; Esc cierra solo el modal superior | Inspección del código | El id estaba fijo en el componente y había un listener global por modal | `useId()` y comprobar que es el modal superior antes de cerrar ✅ |
| F05 | `POST /api/productos` con `codigoBarras:""` | `fields.codigoBarras` decía "solo letras, números y guiones" | "es obligatorio" | curl | `@Pattern` con `+` también fallaba con la cadena vacía | Patrón `*`. Lo cubre la prueba `crearProductoValidaReglasYCodigoUnico` ✅ |

## 5. Pruebas no ejecutadas / pendientes

- **Accesibilidad con lector de pantalla**: no se probó con NVDA ni JAWS. Solo se revisaron las etiquetas, el foco visible y el teclado en los modales. **Pendiente.**
- **Contraste**: los colores de design.md se eligieron para ≥ 4.5:1, pero no se midieron con una herramienta automática. **Pendiente.**
- **Concurrencia** (dos cajeros vendiendo el último ítem a la vez): está protegida con `@Version` (bloqueo optimista → 409), pero **no se probó**.
- **Pruebas automatizadas del frontend** (Vitest/Testing Library): no se implementaron.
