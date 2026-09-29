package pe.facturass20.dominio.modelo;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/** Notificación programada para un mes: 80 % o 100 % del límite, o vencimiento de la declaración. */
public record Aviso(YearMonth periodo, TipoAviso tipo, LocalDate fechaProgramada, boolean enviado) {

    public Aviso {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(fechaProgramada, "fechaProgramada");
    }
}
