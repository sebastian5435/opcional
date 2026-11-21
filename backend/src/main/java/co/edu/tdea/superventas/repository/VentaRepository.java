package co.edu.tdea.superventas.repository;

import co.edu.tdea.superventas.domain.DetalleVenta;
import co.edu.tdea.superventas.domain.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    /** Historial: ventas en el rango (con sus detalles, para contar ítems sin N+1). */
    @Query("""
            select distinct v from Venta v left join fetch v.detalles
            where v.fechaHora >= :desde and v.fechaHora < :hasta
            order by v.fechaHora desc
            """)
    List<Venta> buscarEnRango(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select v from Venta v left join fetch v.detalles d left join fetch d.producto where v.id = :id")
    Optional<Venta> buscarConDetalles(@Param("id") Long id);

    /** Base de los indicadores del tablero: líneas de ventas ACTIVAS del rango (RN07). */
    @Query("""
            select d from DetalleVenta d
              join fetch d.venta v
              join fetch d.producto p
              join fetch p.categoria
            where v.estado = co.edu.tdea.superventas.domain.EstadoVenta.ACTIVA
              and v.fechaHora >= :desde and v.fechaHora < :hasta
            """)
    List<DetalleVenta> detallesActivosEnRango(@Param("desde") LocalDateTime desde,
                                              @Param("hasta") LocalDateTime hasta);
}
