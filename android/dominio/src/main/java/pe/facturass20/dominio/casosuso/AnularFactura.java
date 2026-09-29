package pe.facturass20.dominio.casosuso;

import java.util.Objects;

import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.dominio.puertos.FacturaRepositorio;
import pe.facturass20.dominio.puertos.PeriodoRepositorio;

/**
 * Anula una factura de un mes abierto (P12, RF-18): no la borra, guarda el motivo y recalcula el mes.
 */
public final class AnularFactura {

    private final PeriodoRepositorio periodos;
    private final FacturaRepositorio facturas;
    private final DeterminarCategoria determinarCategoria;

    public AnularFactura(PeriodoRepositorio periodos, FacturaRepositorio facturas,
            DeterminarCategoria determinarCategoria) {
        this.periodos = Objects.requireNonNull(periodos);
        this.facturas = Objects.requireNonNull(facturas);
        this.determinarCategoria = Objects.requireNonNull(determinarCategoria);
    }

    /** Devuelve la determinación del mes ya sin la factura anulada. */
    public Determinacion ejecutar(long idFactura, String motivo) {
        FacturaCompra factura = facturas.buscarPorId(idFactura)
                .orElseThrow(() -> new ReglaNegocioException("No se encontró la factura."));
        PeriodoMensual periodo = periodos.buscar(factura.periodo())
                .orElseThrow(() -> new ReglaNegocioException("No se encontró el mes de la factura."));
        FacturaCompra anulada = periodo.anularFactura(idFactura, motivo);
        Determinacion determinacion = determinarCategoria.calcular(periodo);
        facturas.anular(anulada, determinacion);
        return determinacion;
    }
}
