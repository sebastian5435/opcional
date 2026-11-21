package co.edu.tdea.superventas.web;

import co.edu.tdea.superventas.service.ProductoService;
import co.edu.tdea.superventas.service.ProductoService.Filtro;
import co.edu.tdea.superventas.web.dto.ProductoDtos.ProductoDTO;
import co.edu.tdea.superventas.web.dto.ProductoDtos.ProductoRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {
    private final ProductoService service;

    public ProductoController(ProductoService service) { this.service = service; }

    @GetMapping
    public List<ProductoDTO> listar(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) Long categoriaId,
                                    @RequestParam(required = false) Filtro filtro,
                                    @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return service.buscar(q, categoriaId, filtro, incluirInactivos);
    }

    @GetMapping("/{id}")
    public ProductoDTO obtener(@PathVariable Long id) { return service.obtener(id); }

    @PostMapping
    public ResponseEntity<ProductoDTO> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoDTO creado = service.crear(request);
        return ResponseEntity.created(URI.create("/api/productos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public ProductoDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        service.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
