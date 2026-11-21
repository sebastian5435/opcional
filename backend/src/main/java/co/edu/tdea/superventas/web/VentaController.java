package co.edu.tdea.superventas.web;

import co.edu.tdea.superventas.domain.EstadoVenta;
import co.edu.tdea.superventas.domain.MetodoPago;
import co.edu.tdea.superventas.service.VentaService;
import co.edu.tdea.superventas.web.dto.VentaDtos.VentaDTO;
import co.edu.tdea.superventas.web.dto.VentaDtos.VentaRequest;
import co.edu.tdea.superventas.web.dto.VentaDtos.VentaResumenDTO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {
    private final VentaService service;

    public VentaController(VentaService service) { this.service = service; }

    @GetMapping
    public List<VentaResumenDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) MetodoPago metodoPago,
            @RequestParam(required = false) EstadoVenta estado) {
        return service.buscar(desde, hasta, metodoPago, estado);
    }

    @GetMapping("/{id}")
    public VentaDTO obtener(@PathVariable Long id) { return service.obtener(id); }

    @PostMapping
    public ResponseEntity<VentaDTO> registrar(@Valid @RequestBody VentaRequest request) {
        VentaDTO venta = service.registrar(request);
        return ResponseEntity.created(URI.create("/api/ventas/" + venta.id())).body(venta);
    }

    @PostMapping("/{id}/anular")
    public VentaDTO anular(@PathVariable Long id) { return service.anular(id); }
}
