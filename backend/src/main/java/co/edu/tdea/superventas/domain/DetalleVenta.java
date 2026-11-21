package co.edu.tdea.superventas.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "detalle_venta")
public class DetalleVenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal costoUnitario;

    @Column(nullable = false)
    private int ivaPorcentaje;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal iva;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    protected DetalleVenta() {}

    /** Crea la línea congelando el precio, el costo y el IVA actuales del producto (RN02, RN06). */
    public static DetalleVenta de(Producto p, int cantidad) {
        DetalleVenta d = new DetalleVenta();
        d.producto = p;
        d.cantidad = cantidad;
        d.precioUnitario = p.getPrecioVenta();
        d.costoUnitario = p.getPrecioCompra();
        d.ivaPorcentaje = p.getIvaPorcentaje();
        d.subtotal = p.getPrecioVenta().multiply(BigDecimal.valueOf(cantidad)).setScale(0, RoundingMode.HALF_UP);
        d.iva = d.subtotal.multiply(BigDecimal.valueOf(d.ivaPorcentaje))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        d.total = d.subtotal.add(d.iva);
        return d;
    }

    public Long getId() { return id; }
    public Venta getVenta() { return venta; }
    void setVenta(Venta venta) { this.venta = venta; }
    public Producto getProducto() { return producto; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public int getIvaPorcentaje() { return ivaPorcentaje; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getIva() { return iva; }
    public BigDecimal getTotal() { return total; }
}
