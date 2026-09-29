package pe.facturass20.dominio.casosuso;

import java.util.Objects;

import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.ImagenFactura;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;
import pe.facturass20.dominio.puertos.FacturaRepositorio;
import pe.facturass20.dominio.puertos.PeriodoRepositorio;
import pe.facturass20.dominio.puertos.Reloj;
import pe.facturass20.dominio.reglas.DetectorDuplicados;
import pe.facturass20.dominio.reglas.ValidadorFecha;

/**
 * Guarda una factura ya verificada en su mes y recalcula la categoría (RF-08). La factura, su imagen,
 * sus campos y la determinación se guardan en una sola transacción (RNF-13).
 */
public final class RegistrarFactura {

    /** Lo que muestra P10: la factura guardada (con id) y el acumulado del mes. */
    public record Resultado(FacturaCompra factura, Determinacion determinacion) { }

    private final PeriodoRepositorio periodos;
    private final FacturaRepositorio facturas;
    private final DeterminarCategoria determinarCategoria;
    private final Reloj reloj;

    public RegistrarFactura(PeriodoRepositorio periodos, FacturaRepositorio facturas,
            DeterminarCategoria determinarCategoria, Reloj reloj) {
        this.periodos = Objects.requireNonNull(periodos);
        this.facturas = Objects.requireNonNull(facturas);
        this.determinarCategoria = Objects.requireNonNull(determinarCategoria);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /**
     * @param factura            factura nueva, normalmente la de {@link VerificarFactura.Resultado#factura()}
     * @param imagen             nula si se registró a mano sin foto
     * @param extraccion         nula si se registró a mano
     * @param periodoConfirmado  el usuario confirmó registrar una factura de un mes antiguo
     * @throws FacturaDuplicadaException si ya existe una vigente con la misma clave
     */
    public Resultado ejecutar(FacturaCompra factura, ImagenFactura imagen, ResultadoExtraccion extraccion,
            boolean periodoConfirmado) {
        if (factura.id() != null) {
            throw new IllegalArgumentException("La factura ya está guardada: " + factura.id());
        }
        DetectorDuplicados.buscarDuplicado(factura,
                        facturas.buscarPorEmisorYSerie(factura.emisor().ruc(), factura.serie()))
                .ifPresent(existente -> {
                    throw new FacturaDuplicadaException(existente);
                });

        PeriodoMensual periodo = periodos.buscar(factura.periodo())
                .orElseGet(() -> PeriodoMensual.nuevo(factura.periodo()));
        switch (ValidadorFecha.evaluar(factura.fechaEmision(), reloj.hoy(), periodo.estado())) {
            case FUTURA -> throw new ReglaNegocioException("La fecha no puede ser posterior a hoy.");
            case PERIODO_CERRADO -> throw new ReglaNegocioException("El mes de esta factura ya está cerrado.");
            case REQUIERE_CONFIRMACION -> {
                if (!periodoConfirmado) {
                    throw new ReglaNegocioException("Confirme el mes de la factura antes de guardarla.");
                }
            }
            default -> { }
        }

        periodo.agregarFactura(factura);
        Determinacion determinacion = determinarCategoria.calcular(periodo);
        long id = facturas.registrar(periodo, factura, imagen, extraccion, determinacion);
        factura.asignarId(id);
        return new Resultado(factura, determinacion);
    }
}
