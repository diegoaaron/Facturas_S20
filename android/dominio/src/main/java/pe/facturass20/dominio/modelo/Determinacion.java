package pe.facturass20.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Resultado del motor de reglas para un mes (documentación técnica §5.4). Es inmutable y hay una por
 * período; al cerrar el mes queda congelada.
 *
 * @param categoria       nula si el monto supera todas las categorías ({@link NivelAlerta#FUERA_DE_REGIMEN})
 * @param cuota           nula en el mismo caso
 * @param avisoTopeAnual  el acumulado del año (compras o ventas) llegó al umbral del tope anual
 */
public record Determinacion(YearMonth periodo, BigDecimal totalAdquisiciones, BigDecimal montoDeterminante,
        CategoriaNRUS categoria, BigDecimal cuota, LocalDate fechaVencimiento, NivelAlerta nivelAlerta,
        boolean avisoTopeAnual) {

    public Determinacion {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(totalAdquisiciones, "totalAdquisiciones");
        Objects.requireNonNull(montoDeterminante, "montoDeterminante");
        Objects.requireNonNull(fechaVencimiento, "fechaVencimiento");
        Objects.requireNonNull(nivelAlerta, "nivelAlerta");
        if ((categoria == null) != (nivelAlerta == NivelAlerta.FUERA_DE_REGIMEN)) {
            throw new IllegalArgumentException("Solo FUERA_DE_REGIMEN va sin categoría");
        }
    }

    public boolean fueraDeRegimen() {
        return nivelAlerta == NivelAlerta.FUERA_DE_REGIMEN;
    }
}
