package co.edu.tdea.superventas.web.dto;

import co.edu.tdea.superventas.domain.Categoria;
import co.edu.tdea.superventas.domain.Producto;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ProductoDtos {
    private ProductoDtos() {}

    public record CategoriaDTO(Long id, String nombre) {
        public static CategoriaDTO de(Categoria c) { return new CategoriaDTO(c.getId(), c.getNombre()); }
    }

    public record ProductoRequest(
            @NotBlank(message = "es obligatorio") @Size(max = 32, message = "máximo 32 caracteres")
            @Pattern(regexp = "^[0-9A-Za-z-]*$", message = "solo letras, números y guiones")
            String codigoBarras,
            @NotBlank(message = "es obligatorio") @Size(max = 120, message = "máximo 120 caracteres")
            String nombre,
            @NotNull(message = "es obligatoria") Long categoriaId,
            @NotNull(message = "es obligatorio") @PositiveOrZero(message = "no puede ser negativo")
            @Digits(integer = 12, fraction = 2, message = "formato de monto inválido")
            BigDecimal precioCompra,
            @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que 0")
            @Digits(integer = 12, fraction = 2, message = "formato de monto inválido")
            BigDecimal precioVenta,
            @NotNull(message = "es obligatorio") Integer ivaPorcentaje,
            @NotNull(message = "es obligatorio") @PositiveOrZero(message = "no puede ser negativo")
            @Max(value = 1_000_000, message = "valor demasiado alto")
            Integer stockActual,
            @NotNull(message = "es obligatorio") @PositiveOrZero(message = "no puede ser negativo")
            @Max(value = 1_000_000, message = "valor demasiado alto")
            Integer stockMinimo,
            LocalDate fechaVencimiento,
            Boolean activo
    ) {}

    public record ProductoDTO(Long id, String codigoBarras, String nombre, Long categoriaId, String categoriaNombre,
                              BigDecimal precioCompra, BigDecimal precioVenta, int ivaPorcentaje,
                              int stockActual, int stockMinimo, LocalDate fechaVencimiento, boolean activo,
                              boolean bajoStock, boolean porVencer, boolean vencido) {
        public static ProductoDTO de(Producto p, LocalDate hoy) {
            return new ProductoDTO(p.getId(), p.getCodigoBarras(), p.getNombre(),
                    p.getCategoria().getId(), p.getCategoria().getNombre(),
                    p.getPrecioCompra(), p.getPrecioVenta(), p.getIvaPorcentaje(),
                    p.getStockActual(), p.getStockMinimo(), p.getFechaVencimiento(), p.isActivo(),
                    p.isBajoStock(), p.isPorVencer(hoy), p.isVencido(hoy));
        }
    }
}
