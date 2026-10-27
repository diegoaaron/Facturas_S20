package pe.facturass20.datos.repositorios;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.entidades.ReporteMensualEntity;

/** Los PDF generados de cada mes (tabla {@code reporte_mensual}, RF-15): dónde quedó y su SHA-256. */
public final class ReporteRepositorioRoom {

    private final BaseDatosFacturas db;
    private final PeriodoRepositorioRoom periodos;

    public ReporteRepositorioRoom(BaseDatosFacturas db, PeriodoRepositorioRoom periodos) {
        this.db = db;
        this.periodos = periodos;
    }

    /**
     * Anota un PDF generado. Si el mes todavía no existe en la base (no tiene facturas ni ventas), no anota
     * nada.
     *
     * @return si se anotó
     */
    public boolean registrar(YearMonth periodo, String rutaPdf, String sha256, LocalDateTime fecha) {
        Optional<Long> idPeriodo = periodos.idPeriodo(periodo);
        if (!idPeriodo.isPresent()) {
            return false;
        }
        ReporteMensualEntity fila = new ReporteMensualEntity();
        fila.idPeriodo = idPeriodo.get();
        fila.rutaPdf = rutaPdf;
        fila.sha256 = sha256;
        fila.fechaGeneracion = fecha;
        db.reporteDao().insertar(fila);
        return true;
    }
}
