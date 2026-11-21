package co.edu.tdea.superventas.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardDTO(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal totalVentas,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal utilidadBruta,
        long numeroVentas,
        BigDecimal ticketPromedio,
        List<PuntoDia> ventasPorDia,
        List<Segmento> ventasPorCategoria,
        List<Segmento> ventasPorMetodoPago,
        List<TopProducto> topProductos,
        Alertas alertas) {

    public record PuntoDia(LocalDate fecha, BigDecimal total) {}

    /** {@code cantidad} = número de ventas del segmento (para medios de pago) o unidades (categorías). */
    public record Segmento(String nombre, BigDecimal total, long cantidad) {}

    public record TopProducto(Long productoId, String nombre, long unidades, BigDecimal total) {}

    public record Alertas(long bajoStock, long porVencer, long vencidos) {}
}
