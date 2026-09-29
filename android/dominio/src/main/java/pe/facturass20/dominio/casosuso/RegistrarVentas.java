package pe.facturass20.dominio.casosuso;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.puertos.PeriodoRepositorio;

/** Guarda el total de ventas del mes (P13, RF-09) y recalcula la categoría. */
public final class RegistrarVentas {

    private final PeriodoRepositorio periodos;
    private final DeterminarCategoria determinarCategoria;

    public RegistrarVentas(PeriodoRepositorio periodos, DeterminarCategoria determinarCategoria) {
        this.periodos = Objects.requireNonNull(periodos);
        this.determinarCategoria = Objects.requireNonNull(determinarCategoria);
    }

    public Determinacion ejecutar(YearMonth mes, BigDecimal totalVentas) {
        PeriodoMensual periodo = periodos.buscar(mes).orElseGet(() -> PeriodoMensual.nuevo(mes));
        periodo.registrarVentas(totalVentas);
        Determinacion determinacion = determinarCategoria.calcular(periodo);
        periodos.guardar(periodo, determinacion);
        return determinacion;
    }
}
