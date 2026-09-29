package pe.facturass20.dominio.casosuso;

import java.time.YearMonth;
import java.util.Objects;

import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.dominio.puertos.PeriodoRepositorio;

/**
 * Cierra un mes (P17, RF-17): exige las ventas registradas y congela su determinación. Generar el
 * reporte PDF y sugerir la exportación se agregan en la iteración I5.
 */
public final class CerrarPeriodo {

    private final PeriodoRepositorio periodos;
    private final DeterminarCategoria determinarCategoria;

    public CerrarPeriodo(PeriodoRepositorio periodos, DeterminarCategoria determinarCategoria) {
        this.periodos = Objects.requireNonNull(periodos);
        this.determinarCategoria = Objects.requireNonNull(determinarCategoria);
    }

    /** Devuelve la determinación final, la que queda congelada. */
    public Determinacion ejecutar(YearMonth mes) {
        PeriodoMensual periodo = periodos.buscar(mes)
                .orElseThrow(() -> new ReglaNegocioException("Antes de cerrar el mes, registre sus ventas."));
        periodo.cerrar();
        Determinacion determinacion = determinarCategoria.calcular(periodo);
        periodos.guardar(periodo, determinacion);
        return determinacion;
    }
}
