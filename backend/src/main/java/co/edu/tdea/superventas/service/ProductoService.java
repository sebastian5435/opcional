package co.edu.tdea.superventas.service;

import co.edu.tdea.superventas.domain.Categoria;
import co.edu.tdea.superventas.domain.Producto;
import co.edu.tdea.superventas.error.ApiException;
import co.edu.tdea.superventas.repository.CategoriaRepository;
import co.edu.tdea.superventas.repository.ProductoRepository;
import co.edu.tdea.superventas.web.dto.ProductoDtos.CategoriaDTO;
import co.edu.tdea.superventas.web.dto.ProductoDtos.ProductoDTO;
import co.edu.tdea.superventas.web.dto.ProductoDtos.ProductoRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@Service
@Transactional(readOnly = true)
public class ProductoService {

    /** RN03. */
    public static final Set<Integer> IVAS_PERMITIDOS = Set.of(0, 5, 19);

    public enum Filtro { BAJO_STOCK, POR_VENCER, VENCIDOS }

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final Clock clock;

    public ProductoService(ProductoRepository productos, CategoriaRepository categorias, Clock clock) {
        this.productos = productos;
        this.categorias = categorias;
        this.clock = clock;
    }

    public List<CategoriaDTO> categorias() {
        return categorias.findAll(Sort.by("nombre")).stream().map(CategoriaDTO::de).toList();
    }

    public List<ProductoDTO> buscar(String q, Long categoriaId, Filtro filtro, boolean incluirInactivos) {
        LocalDate hoy = LocalDate.now(clock);
        String patron = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        Predicate<Producto> condicion = switch (filtro) {
            case null -> p -> true;
            case BAJO_STOCK -> Producto::isBajoStock;
            case POR_VENCER -> p -> p.isPorVencer(hoy);
            case VENCIDOS -> p -> p.isVencido(hoy);
        };
        return productos.buscar(patron, categoriaId, incluirInactivos).stream()
                .filter(condicion)
                .map(p -> ProductoDTO.de(p, hoy))
                .toList();
    }

    public ProductoDTO obtener(Long id) {
        return ProductoDTO.de(buscarEntidad(id), LocalDate.now(clock));
    }

    @Transactional
    public ProductoDTO crear(ProductoRequest r) {
        validarReglas(r);
        if (productos.existsByCodigoBarras(r.codigoBarras().trim())) {
            throw ApiException.conflict("Ya existe un producto con el código de barras " + r.codigoBarras().trim());
        }
        Producto p = new Producto(r.codigoBarras().trim(), r.nombre().trim(), categoria(r.categoriaId()),
                r.precioCompra(), r.precioVenta(), r.ivaPorcentaje(), r.stockActual(), r.stockMinimo(),
                r.fechaVencimiento());
        if (r.activo() != null) p.setActivo(r.activo());
        return ProductoDTO.de(productos.save(p), LocalDate.now(clock));
    }

    @Transactional
    public ProductoDTO actualizar(Long id, ProductoRequest r) {
        Producto p = buscarEntidad(id);
        validarReglas(r);
        if (productos.existsByCodigoBarrasAndIdNot(r.codigoBarras().trim(), id)) {
            throw ApiException.conflict("Ya existe otro producto con el código de barras " + r.codigoBarras().trim());
        }
        p.setCodigoBarras(r.codigoBarras().trim());
        p.setNombre(r.nombre().trim());
        p.setCategoria(categoria(r.categoriaId()));
        p.setPrecioCompra(r.precioCompra());
        p.setPrecioVenta(r.precioVenta());
        p.setIvaPorcentaje(r.ivaPorcentaje());
        p.setStockActual(r.stockActual());
        p.setStockMinimo(r.stockMinimo());
        p.setFechaVencimiento(r.fechaVencimiento());
        if (r.activo() != null) p.setActivo(r.activo());
        return ProductoDTO.de(p, LocalDate.now(clock));
    }

    /** RN13: desactivación lógica. */
    @Transactional
    public void desactivar(Long id) {
        buscarEntidad(id).setActivo(false);
    }

    Producto buscarEntidad(Long id) {
        return productos.findById(id)
                .orElseThrow(() -> ApiException.notFound("No existe el producto con id " + id));
    }

    private Categoria categoria(Long id) {
        return categorias.findById(id)
                .orElseThrow(() -> ApiException.invalid(Map.of("categoriaId", "la categoría no existe")));
    }

    /** RN03 y RN12 (las validaciones de forma las hace Bean Validation en el DTO). */
    private void validarReglas(ProductoRequest r) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (!IVAS_PERMITIDOS.contains(r.ivaPorcentaje())) {
            errores.put("ivaPorcentaje", "debe ser 0, 5 o 19");
        }
        if (r.precioVenta().compareTo(r.precioCompra()) < 0) {
            errores.put("precioVenta", "no puede ser menor que el precio de compra");
        }
        if (!errores.isEmpty()) throw ApiException.invalid(errores);
    }
}
