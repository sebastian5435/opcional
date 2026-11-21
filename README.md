# SuperVentas — Gestión de ventas de un supermercado

Prototipo web hecho para el laboratorio *"Construcción asistida por IA de una aplicación web"* (Fundamentos de Sistemas de Información). Caso de estudio: **gestión de ventas de un supermercado de barrio**.

- **Backend:** Spring Boot 4.2.0 (Java, `release 21`), API REST, JPA, H2 en memoria y 20 productos de prueba.
- **Frontend:** React 19.3.1, React Router 7.18.6 y Vite 8.3.8.
- **Sin autenticación** (decisión del MVP).

## Documentos

| Archivo | Contenido |
|---|---|
| [PRD.md](PRD.md) | Problema, usuarios, alcance, reglas de negocio, requisitos y flujos |
| [design.md](design.md) | Sistema visual (paleta, tipografía, componentes) |
| [SPEC-TECNICA.md](SPEC-TECNICA.md) | Arquitectura, modelo de datos, API, versiones y plan de pruebas |
| [PRUEBAS.md](PRUEBAS.md) | Matriz de pruebas con los resultados reales y el registro de fallos |
| [CALIDAD.md](CALIDAD.md) | Revisión de calidad, trazabilidad y deuda técnica |
| [mockups/](mockups/) | Mockups de Google Stitch (instrucciones y prompt) |
| [docs/capturas/](docs/capturas/) | Capturas de evidencia del prototipo funcionando |

## Requisitos

| Herramienta | Versión probada | Verificar con |
|---|---|---|
| Java JDK | 21 o superior (probado con 26.0.2) | `java -version` |
| Maven | No hace falta instalarlo: se usa el **Maven Wrapper** incluido (Maven 3.10.1) | — |
| Node.js / npm | 24.21.0 / 11.19.0 | `node -v`, `npm -v` |
| Navegador | Chrome, Edge o Firefox actualizado | — |

## Ejecución

### 1. Backend (puerto 8080)

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

En Linux o macOS: `./mvnw spring-boot:run`. Espera a ver `Datos de prueba cargados: 6 categorías, 20 productos, … ventas` en el log.

- API: <http://localhost:8080/api/productos>
- Consola H2 (solo desarrollo): <http://localhost:8080/h2-console>, con JDBC URL `jdbc:h2:mem:superventas`, usuario `sa` y contraseña vacía.

### 2. Frontend (puerto 5173)

```powershell
cd frontend
npm install
npm run dev
```

Abre la URL que muestre Vite (normalmente <http://localhost:5173>). Vite redirige `/api` al backend en el puerto 8080 (`vite.config.js`).

### 3. Pruebas

```powershell
cd backend
.\mvnw.cmd test
```

```powershell
cd frontend
npm run build
npm run lint
```

## Configuración

| Propiedad (`backend/src/main/resources/application.properties`) | Valor | Descripción |
|---|---|---|
| `server.port` | `8080` | Puerto de la API. Si lo cambias, actualiza también el proxy en `frontend/vite.config.js` |
| `app.seed.enabled` | `true` | Carga los datos ficticios al arrancar |
| `app.cors.allowed-origins` | `http://localhost:5173` | Origen permitido si el frontend no usa el proxy |
| `spring.h2.console.enabled` | `true` | **Desactivar fuera del entorno local** |

## API (resumen)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/dashboard?desde&hasta` | Indicadores del periodo (por defecto, el mes en curso) |
| GET | `/api/categorias` | Categorías |
| GET | `/api/productos?q&categoriaId&filtro&incluirInactivos` | Catálogo; `filtro` = `BAJO_STOCK`, `POR_VENCER` o `VENCIDOS` |
| GET/POST/PUT/DELETE | `/api/productos[/{id}]` | CRUD (DELETE = desactivar) |
| GET | `/api/ventas?desde&hasta&metodoPago&estado` | Historial |
| GET/POST | `/api/ventas[/{id}]` | Detalle y registro de una venta |
| POST | `/api/ventas/{id}/anular` | Anulación (restaura el stock) |

El detalle de parámetros, respuestas y errores está en [SPEC-TECNICA.md](SPEC-TECNICA.md#42-api-rest-base-api).

## Estructura

```
supermercado-ventas/
├── PRD.md · design.md · SPEC-TECNICA.md · PRUEBAS.md · CALIDAD.md · README.md
├── mockups/            ← exportación de Google Stitch (pendiente, ver mockups/README.md)
├── docs/capturas/      ← evidencias
├── backend/            ← Spring Boot (domain, repository, service, web, error, config)
└── frontend/           ← React + Vite (api, components, hooks, pages, utils)
```

## Limitaciones conocidas

- **Los datos se pierden al reiniciar el backend** (H2 en memoria). Los datos de prueba son ficticios y se regeneran con una semilla fija.
- Sin autenticación: no es apto para datos reales.
- Ver la deuda técnica completa en [CALIDAD.md](CALIDAD.md).
