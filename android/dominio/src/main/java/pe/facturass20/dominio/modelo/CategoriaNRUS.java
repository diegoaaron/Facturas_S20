package pe.facturass20.dominio.modelo;

import java.math.BigDecimal;
import java.util.Objects;

import pe.facturass20.dominio.reglas.Montos;

/** Categoría del NRUS: límite mensual de compras o ventas y cuota que se paga. */
public record CategoriaNRUS(int codigo, BigDecimal limiteMensual, BigDecimal cuota) {

    public CategoriaNRUS {
        limiteMensual = Montos.normalizar(Objects.requireNonNull(limiteMensual, "limiteMensual"));
        cuota = Montos.normalizar(Objects.requireNonNull(cuota, "cuota"));
    }

    /** El límite es inclusivo: S/ 5 000,00 todavía es categoría 1. */
    public boolean admite(BigDecimal monto) {
        return monto.compareTo(limiteMensual) <= 0;
    }
}
