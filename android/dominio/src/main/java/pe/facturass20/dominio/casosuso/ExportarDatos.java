package pe.facturass20.dominio.casosuso;

import java.io.IOException;
import java.io.OutputStream;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pe.facturass20.dominio.modelo.DatosExportacion;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.dominio.modelo.ResumenExportacion;
import pe.facturass20.dominio.modelo.SolicitudExportacion;
import pe.facturass20.dominio.puertos.ExportadorArchivos;
import pe.facturass20.dominio.puertos.PeriodoRepositorio;

/**
 * Exporta un mes o un año (P20, RF-21, documentación técnica §9): reúne los meses con datos y sus
 * determinaciones y se los pasa al exportador, que escribe el ZIP.
 */
public final class ExportarDatos {

    private final PeriodoRepositorio periodos;
    private final ExportadorArchivos exportador;

    public ExportarDatos(PeriodoRepositorio periodos, ExportadorArchivos exportador) {
        this.periodos = Objects.requireNonNull(periodos);
        this.exportador = Objects.requireNonNull(exportador);
    }

    public ResumenExportacion ejecutar(SolicitudExportacion solicitud, OutputStream destino) throws IOException {
        return exportador.exportar(reunir(solicitud), destino);
    }

    /** Los meses pedidos que tienen facturas o ventas, del más antiguo al más reciente. */
    DatosExportacion reunir(SolicitudExportacion solicitud) {
        List<PeriodoMensual> candidatos = solicitud.esAnual()
                ? periodos.listarDelAnio(solicitud.anio())
                : periodos.buscar(YearMonth.of(solicitud.anio(), solicitud.mes())).map(List::of).orElse(List.of());
        List<PeriodoMensual> conDatos = new ArrayList<>();
        Map<YearMonth, Determinacion> determinaciones = new HashMap<>();
        for (PeriodoMensual periodo : candidatos) {
            if (periodo.facturas().isEmpty() && !periodo.ventasRegistradas()) {
                continue;
            }
            conDatos.add(periodo);
            periodos.buscarDeterminacion(periodo.periodo())
                    .ifPresent(determinacion -> determinaciones.put(periodo.periodo(), determinacion));
        }
        if (conDatos.isEmpty()) {
            throw new ReglaNegocioException("No hay facturas ni ventas registradas en ese período.");
        }
        return new DatosExportacion(solicitud, conDatos, determinaciones);
    }
}
