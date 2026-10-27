package pe.facturass20.datos.repositorios;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.dao.DeterminacionConCategoria;
import pe.facturass20.datos.dao.FacturaConEmisor;
import pe.facturass20.datos.entidades.ParametroVersionEntity;

/**
 * Columnas de la exportación (§9.2) que la base guarda y el dominio no: la fecha en que se registró cada
 * factura y con qué parámetros y cuándo se calculó cada mes.
 */
public final class DetallesExportacionRoom {

    /** @param versionParametros nula si el mes quedó fuera del régimen (sin categoría) */
    public record Calculo(String versionParametros, LocalDateTime fecha) { }

    private final BaseDatosFacturas db;
    private final PeriodoRepositorioRoom periodos;

    public DetallesExportacionRoom(BaseDatosFacturas db, PeriodoRepositorioRoom periodos) {
        this.db = db;
        this.periodos = periodos;
    }

    /** Nula si la factura no existe. */
    public LocalDateTime fechaRegistro(long idFactura) {
        FacturaConEmisor fila = db.facturaDao().buscarPorId(idFactura);
        return fila == null ? null : fila.factura.fechaRegistro;
    }

    public Optional<Calculo> calculo(YearMonth periodo) {
        return periodos.idPeriodo(periodo)
                .map(id -> db.periodoDao().buscarDeterminacion(id))
                .map(this::calculo);
    }

    private Calculo calculo(DeterminacionConCategoria fila) {
        String version = null;
        if (fila.categoria != null) {
            ParametroVersionEntity parametros = db.parametrosDao().buscarPorId(fila.categoria.idParametro);
            version = parametros == null ? null : parametros.version;
        }
        return new Calculo(version, fila.determinacion.fechaCalculo);
    }
}
