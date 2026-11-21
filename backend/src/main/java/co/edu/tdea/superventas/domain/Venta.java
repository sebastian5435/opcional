package co.edu.tdea.superventas.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venta")
public class Venta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String numero;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, length = 80)
    private String cajero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodoPago;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal iva = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoVenta estado = EstadoVenta.ACTIVA;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<DetalleVenta> detalles = new ArrayList<>();

    protected Venta() {}

    public Venta(LocalDateTime fechaHora, String cajero, MetodoPago metodoPago) {
        this.fechaHora = fechaHora;
        this.cajero = cajero;
        this.metodoPago = metodoPago;
    }

    /** Agrega la línea y acumula los totales de la venta (RN04). */
    public void agregarDetalle(DetalleVenta d) {
        d.setVenta(this);
        detalles.add(d);
        subtotal = subtotal.add(d.getSubtotal());
        iva = iva.add(d.getIva());
        total = total.add(d.getTotal());
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getCajero() { return cajero; }
    public MetodoPago getMetodoPago() { return metodoPago; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getIva() { return iva; }
    public BigDecimal getTotal() { return total; }
    public EstadoVenta getEstado() { return estado; }
    public void setEstado(EstadoVenta estado) { this.estado = estado; }
    public List<DetalleVenta> getDetalles() { return detalles; }
}
