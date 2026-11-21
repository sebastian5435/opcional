package co.edu.tdea.superventas;

import co.edu.tdea.superventas.domain.Categoria;
import co.edu.tdea.superventas.domain.Producto;
import co.edu.tdea.superventas.repository.CategoriaRepository;
import co.edu.tdea.superventas.repository.ProductoRepository;
import co.edu.tdea.superventas.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración de la API con H2 en memoria y un reloj fijo (2026-03-15 12:00),
 * sin datos semilla, para verificar las reglas de negocio de PRD.md.
 */
@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
class ApiIntegrationTest {

    static final LocalDate HOY = LocalDate.of(2026, 3, 15);

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(HOY.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        }
    }

    @Autowired MockMvc mvc;
    @Autowired CategoriaRepository categorias;
    @Autowired ProductoRepository productos;
    @Autowired VentaRepository ventas;

    Categoria cat;
    Producto gaseosa;   // 1000 + IVA 19 %, stock 10, mínimo 2
    Producto arroz;     // 2500, IVA 0 %, stock 5, mínimo 5 (bajo stock)
    Producto leche;     // vence en 3 días (por vencer)
    Producto yogurt;    // venció ayer

    @BeforeEach
    void datos() {
        ventas.deleteAll();
        productos.deleteAll();
        categorias.deleteAll();
        cat = categorias.save(new Categoria("General"));
        gaseosa = productos.save(prod("111", "Gaseosa", 600, 1000, 19, 10, 2, null));
        arroz = productos.save(prod("222", "Arroz", 1800, 2500, 0, 5, 5, HOY.plusDays(100)));
        leche = productos.save(prod("333", "Leche", 3000, 4000, 0, 20, 2, HOY.plusDays(3)));
        yogurt = productos.save(prod("444", "Yogurt", 5000, 7000, 19, 20, 2, HOY.minusDays(1)));
    }

    private Producto prod(String cod, String nombre, int compra, int venta, int iva, int stock, int min, LocalDate vence) {
        return new Producto(cod, nombre, cat, BigDecimal.valueOf(compra), BigDecimal.valueOf(venta), iva, stock, min, vence);
    }

    private String venta(String items) {
        return "{\"cajero\":\"Prueba\",\"metodoPago\":\"EFECTIVO\",\"items\":[" + items + "]}";
    }

    private String item(Producto p, int cantidad) {
        return "{\"productoId\":" + p.getId() + ",\"cantidad\":" + cantidad + "}";
    }

    @Test
    void registrarVentaCalculaIvaTotalesYDescuentaStock() throws Exception {
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON)
                        .content(venta(item(gaseosa, 3) + "," + item(arroz, 2))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/ventas/")))
                .andExpect(jsonPath("$.numero", matchesPattern("V-\\d{5}")))
                .andExpect(jsonPath("$.subtotal").value(8000))   // 3*1000 + 2*2500
                .andExpect(jsonPath("$.iva").value(570))         // 3000 * 19 %
                .andExpect(jsonPath("$.total").value(8570))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.detalles", hasSize(2)));

        assertThat(productos.findById(gaseosa.getId()).orElseThrow().getStockActual()).isEqualTo(7);
        assertThat(productos.findById(arroz.getId()).orElseThrow().getStockActual()).isEqualTo(3);
    }

    @Test
    void ventaSinStockDevuelve409YNoModificaNada() throws Exception {
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON)
                        .content(venta(item(gaseosa, 2) + "," + item(arroz, 6))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Stock insuficiente para 'Arroz'")));

        assertThat(productos.findById(gaseosa.getId()).orElseThrow().getStockActual()).isEqualTo(10);
        assertThat(productos.findById(arroz.getId()).orElseThrow().getStockActual()).isEqualTo(5);
        assertThat(ventas.count()).isZero();
    }

    @Test
    void lineasRepetidasSeSumanAlValidarStock() throws Exception {
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON)
                        .content(venta(item(arroz, 3) + "," + item(arroz, 3))))
                .andExpect(status().isConflict());
    }

    @Test
    void ventaSinItemsOConCantidadInvalidaDevuelve400() throws Exception {
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(venta("")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.items").exists());
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(venta(item(gaseosa, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['items[0].cantidad']").exists());
    }

    @Test
    void anularRestauraStockYNoPermiteAnularDosVeces() throws Exception {
        MvcResult r = mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON)
                        .content(venta(item(gaseosa, 4))))
                .andExpect(status().isCreated()).andReturn();
        String location = r.getResponse().getHeader("Location");

        mvc.perform(post(location + "/anular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));
        assertThat(productos.findById(gaseosa.getId()).orElseThrow().getStockActual()).isEqualTo(10);

        mvc.perform(post(location + "/anular"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("ya está anulada")));
    }

    @Test
    void dashboardExcluyeVentasAnuladasYCalculaIndicadores() throws Exception {
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(venta(item(gaseosa, 2))))
                .andExpect(status().isCreated());                       // 2000 + 380 = 2380, costo 1200
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(venta(item(leche, 1))))
                .andExpect(status().isCreated());                       // 4000, costo 3000
        String anulada = mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON)
                        .content(venta(item(arroz, 1))))
                .andReturn().getResponse().getHeader("Location");
        mvc.perform(post(anulada + "/anular")).andExpect(status().isOk());

        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-03-01"))
                .andExpect(jsonPath("$.hasta").value("2026-03-15"))
                .andExpect(jsonPath("$.numeroVentas").value(2))
                .andExpect(jsonPath("$.totalVentas").value(6380))
                .andExpect(jsonPath("$.iva").value(380))
                .andExpect(jsonPath("$.utilidadBruta").value(1800))     // (2000-1200) + (4000-3000)
                .andExpect(jsonPath("$.ticketPromedio").value(3190))
                .andExpect(jsonPath("$.ventasPorDia", hasSize(15)))
                .andExpect(jsonPath("$.topProductos[0].nombre").value("Gaseosa"))
                .andExpect(jsonPath("$.ventasPorMetodoPago[?(@.nombre=='EFECTIVO')].total").value(6380))
                .andExpect(jsonPath("$.alertas.bajoStock").value(1))
                .andExpect(jsonPath("$.alertas.porVencer").value(1))
                .andExpect(jsonPath("$.alertas.vencidos").value(1));
    }

    @Test
    void filtrosDeProductosCumplenLasReglas() throws Exception {
        mvc.perform(get("/api/productos").param("filtro", "BAJO_STOCK"))
                .andExpect(jsonPath("$[*].nombre", contains("Arroz")));
        mvc.perform(get("/api/productos").param("filtro", "POR_VENCER"))
                .andExpect(jsonPath("$[*].nombre", contains("Leche")));
        mvc.perform(get("/api/productos").param("filtro", "VENCIDOS"))
                .andExpect(jsonPath("$[*].nombre", contains("Yogurt")));
        mvc.perform(get("/api/productos").param("q", "LECH"))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/productos").param("filtro", "OTRO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearProductoValidaReglasYCodigoUnico() throws Exception {
        String invalido = """
                {"codigoBarras":"999","nombre":"X","categoriaId":%d,"precioCompra":5000,"precioVenta":4000,
                 "ivaPorcentaje":7,"stockActual":1,"stockMinimo":0}""".formatted(cat.getId());
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.ivaPorcentaje").exists())
                .andExpect(jsonPath("$.fields.precioVenta").exists());

        String duplicado = """
                {"codigoBarras":"111","nombre":"Otra","categoriaId":%d,"precioCompra":100,"precioVenta":200,
                 "ivaPorcentaje":19,"stockActual":1,"stockMinimo":0}""".formatted(cat.getId());
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(duplicado))
                .andExpect(status().isConflict());

        String vacio = "{\"codigoBarras\":\"\",\"nombre\":\"\"}";
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(vacio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.codigoBarras").exists())
                .andExpect(jsonPath("$.fields.categoriaId").exists());
    }

    @Test
    void desactivarProductoImpideVenderlo() throws Exception {
        mvc.perform(delete("/api/productos/" + gaseosa.getId())).andExpect(status().isNoContent());
        mvc.perform(get("/api/productos")).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(venta(item(gaseosa, 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("inactivo")));
    }

    @Test
    void recursosInexistentesDevuelven404SinTraza() throws Exception {
        mvc.perform(get("/api/productos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No existe el producto con id 999999"))
                .andExpect(jsonPath("$.trace").doesNotExist());
        mvc.perform(get("/api/ventas/999999")).andExpect(status().isNotFound());
    }
}
