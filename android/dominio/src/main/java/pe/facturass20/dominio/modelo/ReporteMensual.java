package pe.facturass20.dominio.modelo;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Objects;

/** Reporte PDF generado para un mes (documentación técnica §8.5). */
public record ReporteMensual(YearMonth periodo, LocalDateTime fechaGeneracion, String rutaPdf, String sha256) {

    public ReporteMensual {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(fechaGeneracion, "fechaGeneracion");
        Objects.requireNonNull(rutaPdf, "rutaPdf");
        Objects.requireNonNull(sha256, "sha256");
    }
}
