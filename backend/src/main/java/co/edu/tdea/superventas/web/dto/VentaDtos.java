package co.edu.tdea.superventas.web.dto;

import co.edu.tdea.superventas.domain.DetalleVenta;
import co.edu.tdea.superventas.domain.EstadoVenta;
import co.edu.tdea.superventas.domain.MetodoPago;
import co.edu.tdea.superventas.domain.Venta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class VentaDtos {
    private VentaDtos() {}

    public record ItemRequest(
            @NotNull(message = "es obligatorio") Long productoId,
            @NotNull(message = "es obligatoria") @Min(value = 1, message = "debe ser al menos 1")
            @Max(value = 1000, message = "máximo 1000 unidades por línea")
            Integer cantidad) {}

    public record VentaRequest(
            @NotBlank(message = "es obligatorio") @Size(max = 80, message = "máximo 80 caracteres")
            String cajero,
            @NotNull(message = "es obligatorio") MetodoPago metodoPago,
            @NotEmpty(message = "la venta debe tener al menos un producto") @Size(max = 100, message = "máximo 100 líneas")
            List<@Valid @NotNull ItemRequest> items) {}

    public record DetalleDTO(Long id, Long productoId, String codigoBarras, String productoNombre, int cantidad,
                             BigDecimal precioUnitario, int ivaPorcentaje, BigDecimal subtotal,
                             BigDecimal iva, BigDecimal total) {
        public static DetalleDTO de(DetalleVenta d) {
            return new DetalleDTO(d.getId(), d.getProducto().getId(), d.getProducto().getCodigoBarras(),
                    d.getProducto().getNombre(), d.getCantidad(), d.getPrecioUnitario(), d.getIvaPorcentaje(),
                    d.getSubtotal(), d.getIva(), d.getTotal());
        }
    }

    public record VentaDTO(Long id, String numero, LocalDateTime fechaHora, String cajero, MetodoPago metodoPago,
                           BigDecimal subtotal, BigDecimal iva, BigDecimal total, EstadoVenta estado,
                           List<DetalleDTO> detalles) {
        public static VentaDTO de(Venta v) {
            return new VentaDTO(v.getId(), v.getNumero(), v.getFechaHora(), v.getCajero(), v.getMetodoPago(),
                    v.getSubtotal(), v.getIva(), v.getTotal(), v.getEstado(),
                    v.getDetalles().stream().map(DetalleDTO::de).toList());
        }
    }

    public record VentaResumenDTO(Long id, String numero, LocalDateTime fechaHora, String cajero,
                                  MetodoPago metodoPago, int items, BigDecimal total, EstadoVenta estado) {
        public static VentaResumenDTO de(Venta v) {
            int unidades = v.getDetalles().stream().mapToInt(DetalleVenta::getCantidad).sum();
            return new VentaResumenDTO(v.getId(), v.getNumero(), v.getFechaHora(), v.getCajero(),
                    v.getMetodoPago(), unidades, v.getTotal(), v.getEstado());
        }
    }
}
