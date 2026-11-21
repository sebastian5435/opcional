package co.edu.tdea.superventas.config;

import co.edu.tdea.superventas.domain.Categoria;
import co.edu.tdea.superventas.domain.MetodoPago;
import co.edu.tdea.superventas.domain.Producto;
import co.edu.tdea.superventas.repository.CategoriaRepository;
import co.edu.tdea.superventas.repository.ProductoRepository;
import co.edu.tdea.superventas.service.VentaService;
import co.edu.tdea.superventas.web.dto.VentaDtos.ItemRequest;
import co.edu.tdea.superventas.web.dto.VentaDtos.VentaDTO;
import co.edu.tdea.superventas.web.dto.VentaDtos.VentaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Datos ficticios de prueba (RNF07): 6 categorías, 20 productos y ventas de los
 * últimos 30 días. Usa una semilla fija para que los datos sean reproducibles.
 * Las ventas se registran a través de VentaService para respetar las mismas reglas.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String[] CAJEROS = {"Laura Gómez", "Andrés Pérez", "Camila Ruiz"};

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final VentaService ventaService;
    private final Clock clock;

    public DataSeeder(CategoriaRepository categorias, ProductoRepository productos,
                      VentaService ventaService, Clock clock) {
        this.categorias = categorias;
        this.productos = productos;
        this.ventaService = ventaService;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (productos.count() > 0) return;
        LocalDate hoy = LocalDate.now(clock);

        Categoria abarrotes = categorias.save(new Categoria("Abarrotes"));
        Categoria lacteos = categorias.save(new Categoria("Lácteos y huevos"));
        Categoria bebidas = categorias.save(new Categoria("Bebidas"));
        Categoria aseo = categorias.save(new Categoria("Aseo"));
        Categoria fruver = categorias.save(new Categoria("Frutas y verduras"));
        Categoria panaderia = categorias.save(new Categoria("Panadería"));

        List<Producto> lista = List.of(
                p("7702001000011", "Arroz Diana 500 g", abarrotes, 1900, 2500, 0, 140, 20, hoy.plusDays(300)),
                p("7702001000028", "Aceite Premier 1 L", abarrotes, 9800, 12500, 19, 60, 10, hoy.plusDays(240)),
                p("7702001000035", "Azúcar Manuelita 1 kg", abarrotes, 3600, 4600, 5, 80, 15, hoy.plusDays(365)),
                p("7702001000042", "Café Sello Rojo 250 g", abarrotes, 7200, 9200, 5, 8, 10, hoy.plusDays(200)),
                p("7702001000059", "Pasta Doria 250 g", abarrotes, 1700, 2300, 5, 90, 15, hoy.plusDays(400)),
                p("7702001000066", "Leche entera 1 L", lacteos, 3300, 4200, 0, 110, 25, hoy.plusDays(6)),
                p("7702001000073", "Huevos AA x 30", lacteos, 14500, 18500, 0, 45, 8, hoy.plusDays(12)),
                p("7702001000080", "Queso campesino 500 g", lacteos, 9000, 12000, 0, 6, 5, hoy.plusDays(4)),
                p("7702001000097", "Yogurt de fresa 1 L", lacteos, 6200, 8200, 19, 30, 8, hoy.minusDays(2)),
                p("7702001000103", "Gaseosa cola 1.5 L", bebidas, 3200, 4500, 19, 110, 20, hoy.plusDays(150)),
                p("7702001000110", "Agua sin gas 600 ml", bebidas, 900, 1500, 19, 130, 24, hoy.plusDays(360)),
                p("7702001000127", "Jugo de mango 1 L", bebidas, 3000, 4100, 19, 60, 10, hoy.plusDays(90)),
                p("7702001000134", "Cerveza lata 330 ml", bebidas, 2300, 3200, 19, 12, 24, hoy.plusDays(120)),
                p("7702001000141", "Detergente en polvo 1 kg", aseo, 10500, 14500, 19, 45, 8, null),
                p("7702001000158", "Jabón de barra x3", aseo, 4300, 5900, 19, 55, 10, null),
                p("7702001000165", "Papel higiénico x4", aseo, 6800, 9300, 19, 50, 10, null),
                p("7702001000172", "Crema dental 75 ml", aseo, 4100, 5800, 19, 4, 6, null),
                p("7702001000189", "Banano (kg)", fruver, 2200, 3200, 0, 70, 15, hoy.plusDays(5)),
                p("7702001000196", "Tomate chonto (kg)", fruver, 2800, 4000, 0, 60, 10, hoy.plusDays(7)),
                p("7702001000202", "Pan tajado 500 g", panaderia, 4600, 6300, 19, 35, 6, hoy.plusDays(3))
        );
        productos.saveAll(lista);
        List<Long> ids = lista.stream().map(Producto::getId).toList();

        Random rnd = new Random(42);
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<Long> registradas = new ArrayList<>();
        for (int diasAtras = 30; diasAtras >= 0; diasAtras--) {
            LocalDate dia = hoy.minusDays(diasAtras);
            int ventasDia = 1 + rnd.nextInt(3);
            for (int i = 0; i < ventasDia; i++) {
                LocalDateTime fecha = dia.atTime(8 + rnd.nextInt(12), rnd.nextInt(60));
                if (fecha.isAfter(ahora)) fecha = ahora.minusMinutes(1 + i);
                if (fecha.toLocalDate().isBefore(dia)) continue;

                List<ItemRequest> items = new ArrayList<>();
                Set<Long> usados = new HashSet<>();
                int lineas = 1 + rnd.nextInt(4);
                for (int k = 0; k < lineas; k++) {
                    Long id = ids.get(rnd.nextInt(ids.size()));
                    int cantidad = 1 + rnd.nextInt(3);
                    Producto actual = productos.findById(id).orElseThrow();
                    // Mantener las alertas de stock tal como se diseñaron: solo vender con holgura.
                    if (usados.add(id) && actual.getStockActual() - cantidad > actual.getStockMinimo()
                            && !actual.isVencido(dia)) {
                        items.add(new ItemRequest(id, cantidad));
                    }
                }
                if (items.isEmpty()) continue;
                MetodoPago metodo = rnd.nextInt(10) < 5 ? MetodoPago.EFECTIVO
                        : rnd.nextInt(10) < 6 ? MetodoPago.TARJETA : MetodoPago.TRANSFERENCIA;
                VentaDTO v = ventaService.registrar(
                        new VentaRequest(CAJEROS[rnd.nextInt(CAJEROS.length)], metodo, items), fecha);
                registradas.add(v.id());
            }
        }
        // Dos ventas anuladas para demostrar RN07.
        if (registradas.size() > 20) {
            ventaService.anular(registradas.get(4));
            ventaService.anular(registradas.get(registradas.size() - 3));
        }
        log.info("Datos de prueba cargados: {} categorías, {} productos, {} ventas",
                categorias.count(), productos.count(), registradas.size());
    }

    private static Producto p(String codigo, String nombre, Categoria c, int compra, int venta, int iva,
                              int stock, int minimo, LocalDate vence) {
        return new Producto(codigo, nombre, c, BigDecimal.valueOf(compra), BigDecimal.valueOf(venta),
                iva, stock, minimo, vence);
    }
}
