# Revisión de calidad del código generado (Fase 8)

## Trazabilidad PRD → código

| Requisito | Implementación | Prueba |
|---|---|---|
| RF01–RF05 Tablero | `DashboardService.resumen`, `DashboardPage.jsx` | `dashboardExcluyeVentasAnuladas…`, P01 |
| RF06–RF07 POS y stock | `VentaService.registrar`, `PosPage.jsx` | `registrarVenta…`, `ventaSinStock…`, P06 |
| RF08–RF09 Catálogo y filtros | `ProductoService.buscar`, `Producto.isBajoStock/isPorVencer/isVencido` | `filtrosDeProductos…`, P02–P04 |
| RF10–RF11 Crear, editar y desactivar | `ProductoService.crear/actualizar/desactivar`, `ProductoForm.jsx` | `crearProducto…`, `desactivarProducto…`, P08 |
| RF12–RF14 Historial y anulación | `VentaService.buscar/anular`, `VentasPage.jsx` | `anularRestauraStock…`, P05, P11 |
| RF15 Exportar a CSV (deseable) | **No implementado** | — |

## Lista de verificación

| Criterio | Evaluación |
|---|---|
| **Separación de responsabilidades** | ✅ Los controladores solo mapean HTTP y `@Valid`; las reglas viven en los servicios y en los métodos del dominio (`Producto.isBajoStock`, `DetalleVenta.de`). En el frontend, `api/` está separado de `pages/` y `components/`. |
| **Validación en el servidor** | ✅ Bean Validation en los DTO, más las reglas RN03, RN05 y RN12 en los servicios. El frontend repite las validaciones solo para dar retroalimentación inmediata. |
| **Manejo de errores** | ✅ `GlobalExceptionHandler` devuelve un JSON uniforme; los 500 dan un mensaje genérico y la traza solo va al log. `server.error.include-stacktrace=never`. |
| **Calidad de datos** | ✅ `BigDecimal` con redondeo `HALF_UP` al peso; IVA por línea; precio, costo e IVA congelados en `DetalleVenta`. Las sumas por categoría y por medio de pago se verificaron contra el total. |
| **Atomicidad** | ✅ `@Transactional` en `registrar`/`anular`; una línea sin stock revierte toda la venta (probado). `@Version` en `Producto` para el bloqueo optimista. |
| **Seguridad** | ⚠️ Sin autenticación (decisión del MVP): cualquiera puede anular ventas o editar precios. La consola H2 está activa con el usuario `sa` y sin contraseña: **solo para uso local**. No hay secretos en el repositorio. No sirve para datos reales. |
| **Mantenibilidad** | ✅ Paquetes por capa, DTO como `record`, un solo lugar para cada regla (las reglas RN08/RN09 viven en la entidad). ⚠️ `ProductoForm.jsx` exporta un componente y una función (aviso de oxlint `only-export-components`). |
| **Pruebas** | ✅ 11 pruebas de integración ejecutadas en verde. ⚠️ Sin pruebas automatizadas del frontend; la interfaz se probó a mano (PRUEBAS.md). |

## Deuda técnica (resolver antes de producción)

1. **Persistencia**: H2 en memoria con `ddl-auto=create-drop`. Hay que migrar a PostgreSQL/MySQL con migraciones (Flyway) y desactivar `/h2-console`.
2. **Autenticación y roles**: cajero y administrador (por ejemplo, Spring Security + JWT); `cajero` debería ser el usuario autenticado y no un texto libre.
3. **Filtros en memoria**: `ProductoService.buscar` aplica RN08/RN09 en Java y `DashboardService` agrega las líneas en memoria. Es suficiente para cientos de registros, pero para volúmenes reales hay que pasarlo a consultas JPQL/SQL agregadas o a una vista materializada.
4. **Historial sin paginación**: `GET /api/ventas` devuelve todo el rango. Hace falta añadir `Pageable`.
5. **Número de venta**: `V-%05d` sale del ID autogenerado. No es un consecutivo fiscal (la facturación electrónica está fuera del alcance).
6. **Zona horaria**: se usa la del servidor (`Clock.systemDefaultZone()`). Debe fijarse en `America/Bogota`.
7. **Documentación OpenAPI/Swagger**: no se agregó para no añadir dependencias sin verificar su compatibilidad con Spring Boot 4.2. La API está documentada en SPEC-TECNICA.md §4.2.
8. **Pruebas del frontend** con Vitest y Testing Library, y pruebas e2e (Playwright).
9. **Accesibilidad**: falta una auditoría con lector de pantalla y una medición automática del contraste.
