package co.edu.tdea.superventas.service;

import co.edu.tdea.superventas.domain.DetalleVenta;
import co.edu.tdea.superventas.domain.MetodoPago;
import co.edu.tdea.superventas.domain.Producto;
import co.edu.tdea.superventas.domain.Venta;
import co.edu.tdea.superventas.error.ApiException;
import co.edu.tdea.superventas.repository.ProductoRepository;
import co.edu.tdea.superventas.repository.VentaRepository;
import co.edu.tdea.superventas.web.dto.DashboardDTO;
import co.edu.tdea.superventas.web.dto.DashboardDTO.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    static final int MAX_DIAS_RANGO = 366;
    static final int TOP_N = 5;

    private final VentaRepository ventas;
    private final ProductoRepository productos;
    private final Clock clock;

    public DashboardService(VentaRepository ventas, ProductoRepository productos, Clock clock) {
        this.ventas = ventas;
        this.productos = productos;
        this.clock = clock;
    }

    public DashboardDTO resumen(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(clock);
        LocalDate d = desde != null ? desde : hoy.withDayOfMonth(1); // RN14
        LocalDate h = hasta != null ? hasta : hoy;
        if (d.isAfter(h)) throw ApiException.badRequest("La fecha 'desde' no puede ser posterior a 'hasta'");
        if (ChronoUnit.DAYS.between(d, h) >= MAX_DIAS_RANGO) {
            throw ApiException.badRequest("El rango máximo es de " + MAX_DIAS_RANGO + " días");
        }

        List<DetalleVenta> lineas = ventas.detallesActivosEnRango(d.atStartOfDay(), h.plusDays(1).atStartOfDay());

        BigDecimal subtotal = sum(lineas.stream().map(DetalleVenta::getSubtotal));
        BigDecimal iva = sum(lineas.stream().map(DetalleVenta::getIva));
        BigDecimal total = subtotal.add(iva);
        // RN10: utilidad = subtotal sin IVA - costo congelado.
        BigDecimal costo = sum(lineas.stream()
                .map(l -> l.getCostoUnitario().multiply(BigDecimal.valueOf(l.getCantidad()))));
        BigDecimal utilidad = subtotal.subtract(costo).setScale(0, RoundingMode.HALF_UP);

        Set<Venta> ventasUnicas = lineas.stream().map(DetalleVenta::getVenta)
                .collect(Collectors.toCollection(() -> Collections.newSetFromMap(new IdentityHashMap<>())));
        long numeroVentas = ventasUnicas.size();
        // RN11
        BigDecimal ticket = numeroVentas == 0 ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(numeroVentas), 0, RoundingMode.HALF_UP);

        return new DashboardDTO(d, h, total, subtotal, iva, utilidad, numeroVentas, ticket,
                porDia(lineas, d, h), porCategoria(lineas), porMetodoPago(ventasUnicas), top(lineas),
                alertas(hoy));
    }

    private List<PuntoDia> porDia(List<DetalleVenta> lineas, LocalDate d, LocalDate h) {
        Map<LocalDate, BigDecimal> acumulado = new TreeMap<>();
        for (LocalDate f = d; !f.isAfter(h); f = f.plusDays(1)) acumulado.put(f, BigDecimal.ZERO);
        lineas.forEach(l -> acumulado.merge(l.getVenta().getFechaHora().toLocalDate(), l.getTotal(), BigDecimal::add));
        return acumulado.entrySet().stream().map(e -> new PuntoDia(e.getKey(), e.getValue())).toList();
    }

    private List<Segmento> porCategoria(List<DetalleVenta> lineas) {
        Map<String, List<DetalleVenta>> grupos = lineas.stream()
                .collect(Collectors.groupingBy(l -> l.getProducto().getCategoria().getNombre()));
        return grupos.entrySet().stream()
                .map(e -> new Segmento(e.getKey(), sum(e.getValue().stream().map(DetalleVenta::getTotal)),
                        e.getValue().stream().mapToLong(DetalleVenta::getCantidad).sum()))
                .sorted(Comparator.comparing(Segmento::total).reversed())
                .toList();
    }

    private List<Segmento> porMetodoPago(Collection<Venta> ventasUnicas) {
        List<Segmento> res = new ArrayList<>();
        for (MetodoPago m : MetodoPago.values()) {
            List<Venta> vs = ventasUnicas.stream().filter(v -> v.getMetodoPago() == m).toList();
            res.add(new Segmento(m.name(), sum(vs.stream().map(Venta::getTotal)), vs.size()));
        }
        return res;
    }

    private List<TopProducto> top(List<DetalleVenta> lineas) {
        Map<Producto, List<DetalleVenta>> grupos = lineas.stream()
                .collect(Collectors.groupingBy(DetalleVenta::getProducto, IdentityHashMap::new, Collectors.toList()));
        return grupos.entrySet().stream()
                .map(e -> new TopProducto(e.getKey().getId(), e.getKey().getNombre(),
                        e.getValue().stream().mapToLong(DetalleVenta::getCantidad).sum(),
                        sum(e.getValue().stream().map(DetalleVenta::getTotal))))
                .sorted(Comparator.comparingLong(TopProducto::unidades).reversed()
                        .thenComparing(TopProducto::total, Comparator.reverseOrder()))
                .limit(TOP_N)
                .toList();
    }

    private Alertas alertas(LocalDate hoy) {
        List<Producto> activos = productos.findByActivoTrue();
        return new Alertas(
                activos.stream().filter(Producto::isBajoStock).count(),
                activos.stream().filter(p -> p.isPorVencer(hoy)).count(),
                activos.stream().filter(p -> p.isVencido(hoy)).count());
    }

    private static BigDecimal sum(java.util.stream.Stream<BigDecimal> s) {
        return s.reduce(BigDecimal.ZERO, BigDecimal::add).setScale(0, RoundingMode.HALF_UP);
    }
}
