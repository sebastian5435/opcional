package co.edu.tdea.superventas.web;

import co.edu.tdea.superventas.service.ProductoService;
import co.edu.tdea.superventas.web.dto.ProductoDtos.CategoriaDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {
    private final ProductoService service;

    public CategoriaController(ProductoService service) { this.service = service; }

    @GetMapping
    public List<CategoriaDTO> listar() { return service.categorias(); }
}
