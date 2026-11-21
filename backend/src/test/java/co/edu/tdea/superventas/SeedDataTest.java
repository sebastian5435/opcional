package co.edu.tdea.superventas;

import co.edu.tdea.superventas.domain.EstadoVenta;
import co.edu.tdea.superventas.repository.ProductoRepository;
import co.edu.tdea.superventas.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** RNF07: el sistema arranca con al menos 20 productos y ventas recientes. */
@SpringBootTest
class SeedDataTest {

    @Autowired ProductoRepository productos;
    @Autowired VentaRepository ventas;

    @Test
    void cargaDatosDePrueba() {
        assertThat(productos.count()).isEqualTo(20);
        assertThat(ventas.count()).isGreaterThanOrEqualTo(30);
        assertThat(ventas.findAll()).anyMatch(v -> v.getEstado() == EstadoVenta.ANULADA);
        assertThat(productos.findAll()).allMatch(p -> p.getStockActual() >= 0);
    }
}
