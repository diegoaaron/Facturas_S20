package pe.facturass20.dominio.modelo;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Notificación de un mes. {@code fechaProgramada} es el día en que tocaba: para el vencimiento distingue
 * el recordatorio de 5 días del de 1 día; para los demás, el día en que se mandó.
 */
public record Aviso(YearMonth periodo, TipoAviso tipo, LocalDate fechaProgramada, boolean enviado) {

    public Aviso {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(fechaProgramada, "fechaProgramada");
    }
}
