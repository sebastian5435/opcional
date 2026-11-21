package co.edu.tdea.superventas.repository;

import co.edu.tdea.superventas.domain.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarrasAndIdNot(String codigoBarras, Long id);

    /**
     * Búsqueda base del catálogo: texto (nombre o código), categoría y activos.
     * Los filtros derivados de fechas/stock (RN08, RN09) se aplican en el servicio
     * usando los métodos del dominio, para tener una sola definición de cada regla.
     */
    @Query("""
            select p from Producto p join fetch p.categoria c
            where (:incluirInactivos = true or p.activo = true)
              and (:categoriaId is null or c.id = :categoriaId)
              and (:patron is null or lower(p.nombre) like :patron or p.codigoBarras like :patron)
            order by p.nombre
            """)
    List<Producto> buscar(@Param("patron") String patron,
                          @Param("categoriaId") Long categoriaId,
                          @Param("incluirInactivos") boolean incluirInactivos);

    List<Producto> findByActivoTrue();
}
