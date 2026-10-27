package pe.facturass20.dominio.modelo;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lo que va al ZIP de P20 (documentación técnica §9.2): los meses pedidos con todas sus facturas (vigentes y
 * anuladas) y sus determinaciones.
 *
 * @param periodos       del más antiguo al más reciente; solo los que tienen facturas o ventas
 * @param determinaciones por mes; puede faltar la de algún mes
 */
public record DatosExportacion(SolicitudExportacion solicitud, List<PeriodoMensual> periodos,
        Map<YearMonth, Determinacion> determinaciones) {

    public DatosExportacion {
        Objects.requireNonNull(solicitud, "solicitud");
        periodos = List.copyOf(periodos);
        determinaciones = Map.copyOf(determinaciones);
    }

    /** {@code facturas-s20_2026-09.zip} (un mes) o {@code facturas-s20_2026.zip} (el año). */
    public String nombreArchivo() {
        return nombreArchivo(solicitud);
    }

    public static String nombreArchivo(SolicitudExportacion solicitud) {
        String periodo = solicitud.esAnual() ? String.valueOf(solicitud.anio())
                : YearMonth.of(solicitud.anio(), solicitud.mes()).toString();
        return "facturas-s20_" + periodo + ".zip";
    }
}
