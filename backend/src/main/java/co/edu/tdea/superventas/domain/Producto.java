package co.edu.tdea.superventas.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "producto")
public class Producto {
    /** Días hacia adelante que se consideran "por vencer" (RN09). */
    public static final int DIAS_POR_VENCER = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String codigoBarras;

    @Column(nullable = false, length = 120)
    private String nombre;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal precioCompra;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal precioVenta;

    @Column(nullable = false)
    private int ivaPorcentaje;

    @Column(nullable = false)
    private int stockActual;

    @Column(nullable = false)
    private int stockMinimo;

    private LocalDate fechaVencimiento;

    @Column(nullable = false)
    private boolean activo = true;

    @Version
    private Long version;

    protected Producto() {}

    public Producto(String codigoBarras, String nombre, Categoria categoria, BigDecimal precioCompra,
                    BigDecimal precioVenta, int ivaPorcentaje, int stockActual, int stockMinimo,
                    LocalDate fechaVencimiento) {
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.categoria = categoria;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.ivaPorcentaje = ivaPorcentaje;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.fechaVencimiento = fechaVencimiento;
    }

    /** RN08. */
    public boolean isBajoStock() { return stockActual <= stockMinimo; }

    /** RN09. */
    public boolean isVencido(LocalDate hoy) {
        return fechaVencimiento != null && fechaVencimiento.isBefore(hoy);
    }

    /** RN09. */
    public boolean isPorVencer(LocalDate hoy) {
        return fechaVencimiento != null && !fechaVencimiento.isBefore(hoy)
                && !fechaVencimiento.isAfter(hoy.plusDays(DIAS_POR_VENCER));
    }

    public void descontarStock(int cantidad) { this.stockActual -= cantidad; }
    public void reponerStock(int cantidad) { this.stockActual += cantidad; }

    public Long getId() { return id; }
    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public BigDecimal getPrecioCompra() { return precioCompra; }
    public void setPrecioCompra(BigDecimal precioCompra) { this.precioCompra = precioCompra; }
    public BigDecimal getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(BigDecimal precioVenta) { this.precioVenta = precioVenta; }
    public int getIvaPorcentaje() { return ivaPorcentaje; }
    public void setIvaPorcentaje(int ivaPorcentaje) { this.ivaPorcentaje = ivaPorcentaje; }
    public int getStockActual() { return stockActual; }
    public void setStockActual(int stockActual) { this.stockActual = stockActual; }
    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
