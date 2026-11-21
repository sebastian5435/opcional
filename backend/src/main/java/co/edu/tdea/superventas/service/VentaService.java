package co.edu.tdea.superventas.service;

import co.edu.tdea.superventas.domain.*;
import co.edu.tdea.superventas.error.ApiException;
import co.edu.tdea.superventas.repository.ProductoRepository;
import co.edu.tdea.superventas.repository.VentaRepository;
import co.edu.tdea.superventas.web.dto.VentaDtos.ItemRequest;
import co.edu.tdea.superventas.web.dto.VentaDtos.VentaDTO;
import co.edu.tdea.superventas.web.dto.VentaDtos.VentaRequest;
import co.edu.tdea.superventas.web.dto.VentaDtos.VentaResumenDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class VentaService {

    private final VentaRepository ventas;
    private final ProductoRepository productos;
    private final Clock clock;

    public VentaService(VentaRepository ventas, ProductoRepository productos, Clock clock) {
        this.ventas = ventas;
        this.productos = productos;
        this.clock = clock;
    }

    public List<VentaResumenDTO> buscar(LocalDate desde, LocalDate hasta, MetodoPago metodoPago, EstadoVenta estado) {
        LocalDate hoy = LocalDate.now(clock);
        LocalDate d = desde != null ? desde : hoy.minusDays(30);
        LocalDate h = hasta != null ? hasta : hoy;
        if (d.isAfter(h)) throw ApiException.badRequest("La fecha 'desde' no puede ser posterior a 'hasta'");
        return ventas.buscarEnRango(d.atStartOfDay(), h.plusDays(1).atStartOfDay()).stream()
                .filter(v -> metodoPago == null || v.getMetodoPago() == metodoPago)
                .filter(v -> estado == null || v.getEstado() == estado)
                .map(VentaResumenDTO::de)
                .toList();
    }

    public VentaDTO obtener(Long id) {
        return VentaDTO.de(buscarEntidad(id));
    }

    @Transactional
    public VentaDTO registrar(VentaRequest r) {
        return registrar(r, LocalDateTime.now(clock));
    }

    /**
     * Registra una venta de forma atómica (RN05): si alguna línea no tiene stock, se lanza
     * 409 y la transacción completa se revierte. Descuenta stock y congela precios (RN06).
     */
    @Transactional
    public VentaDTO registrar(VentaRequest r, LocalDateTime fechaHora) {
        // Agrupar líneas repetidas del mismo producto para validar el stock total pedido.
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (ItemRequest item : r.items()) {
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }

        Venta venta = new Venta(fechaHora, r.cajero().trim(), r.metodoPago());
        for (Map.Entry<Long, Integer> e : cantidades.entrySet()) {
            Producto p = productos.findById(e.getKey())
                    .orElseThrow(() -> ApiException.notFound("No existe el producto con id " + e.getKey()));
            int cantidad = e.getValue();
            if (!p.isActivo()) {
                throw ApiException.conflict("El producto '" + p.getNombre() + "' está inactivo y no se puede vender");
            }
            if (cantidad > p.getStockActual()) {
                throw ApiException.conflict("Stock insuficiente para '" + p.getNombre() + "': disponible "
                        + p.getStockActual() + ", solicitado " + cantidad);
            }
            p.descontarStock(cantidad);
            venta.agregarDetalle(DetalleVenta.de(p, cantidad));
        }
        Venta guardada = ventas.save(venta);
        guardada.setNumero(String.format("V-%05d", guardada.getId()));
        return VentaDTO.de(guardada);
    }

    /** RN07: anula, restaura stock y la excluye de los indicadores. */
    @Transactional
    public VentaDTO anular(Long id) {
        Venta v = buscarEntidad(id);
        if (v.getEstado() == EstadoVenta.ANULADA) {
            throw ApiException.conflict("La venta " + v.getNumero() + " ya está anulada");
        }
        v.getDetalles().forEach(d -> d.getProducto().reponerStock(d.getCantidad()));
        v.setEstado(EstadoVenta.ANULADA);
        return VentaDTO.de(v);
    }

    private Venta buscarEntidad(Long id) {
        return ventas.buscarConDetalles(id)
                .orElseThrow(() -> ApiException.notFound("No existe la venta con id " + id));
    }
}
