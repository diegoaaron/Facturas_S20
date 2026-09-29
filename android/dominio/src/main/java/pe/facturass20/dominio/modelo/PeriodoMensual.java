package pe.facturass20.dominio.modelo;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import pe.facturass20.dominio.reglas.Montos;

/**
 * Un mes del contribuyente con sus facturas de compra y su total de ventas. Es el agregado que se
 * modifica al registrar, anular, registrar ventas o cerrar; sus reglas están en la documentación
 * técnica §5.1 y §5.5.
 */
public final class PeriodoMensual {

    private final YearMonth periodo;
    private BigDecimal totalVentas;
    private EstadoPeriodo estado;
    private final List<FacturaCompra> facturas;

    /**
     * Constructor completo, para reconstruir un mes guardado.
     *
     * @param totalVentas nulo si todavía no se registraron las ventas del mes
     */
    public PeriodoMensual(YearMonth periodo, BigDecimal totalVentas, EstadoPeriodo estado,
            List<FacturaCompra> facturas) {
        this.periodo = Objects.requireNonNull(periodo, "periodo");
        this.totalVentas = totalVentas == null ? null : Montos.normalizar(totalVentas);
        this.estado = Objects.requireNonNull(estado, "estado");
        this.facturas = new ArrayList<>(facturas);
    }

    /** Mes nuevo: abierto, sin facturas y sin ventas registradas. */
    public static PeriodoMensual nuevo(YearMonth periodo) {
        return new PeriodoMensual(periodo, null, EstadoPeriodo.ABIERTO, List.of());
    }

    /** Suma de las facturas VIGENTE del mes. */
    public BigDecimal totalAdquisiciones() {
        BigDecimal total = Montos.CERO;
        for (FacturaCompra factura : facturas) {
            if (factura.esVigente()) {
                total = total.add(factura.importeTotal());
            }
        }
        return total;
    }

    public void registrarVentas(BigDecimal monto) {
        exigirAbierto();
        Objects.requireNonNull(monto, "monto");
        if (monto.signum() < 0) {
            throw new ReglaNegocioException("Las ventas no pueden ser negativas.");
        }
        totalVentas = Montos.normalizar(monto);
    }

    /** Agrega una factura nueva y vigente de este mismo mes. No comprueba duplicados de otros meses. */
    public void agregarFactura(FacturaCompra factura) {
        exigirAbierto();
        if (!factura.periodo().equals(periodo)) {
            throw new IllegalArgumentException("La factura es de " + factura.periodo() + ", no de " + periodo);
        }
        if (!factura.esVigente()) {
            throw new IllegalArgumentException("Solo se agregan facturas vigentes");
        }
        if (factura.moneda() != Moneda.PEN) {
            throw new ReglaNegocioException("Por ahora solo se registran compras en soles (S/).");
        }
        for (FacturaCompra otra : facturas) {
            if (otra.esVigente() && otra.claveUnica().equals(factura.claveUnica())) {
                throw new ReglaNegocioException("Esta factura ya está registrada.");
            }
        }
        facturas.add(factura);
    }

    /** Anula la factura con ese id; el mes debe estar abierto. Devuelve la factura anulada. */
    public FacturaCompra anularFactura(long idFactura, String motivo) {
        exigirAbierto();
        for (FacturaCompra factura : facturas) {
            if (factura.id() != null && factura.id() == idFactura) {
                factura.anular(motivo);
                return factura;
            }
        }
        throw new ReglaNegocioException("No se encontró la factura en este mes.");
    }

    /** Cierra el mes: exige que las ventas estén registradas. Luego ya no se edita. */
    public void cerrar() {
        exigirAbierto();
        if (totalVentas == null) {
            throw new ReglaNegocioException("Antes de cerrar el mes, registre sus ventas.");
        }
        estado = EstadoPeriodo.CERRADO;
    }

    private void exigirAbierto() {
        if (estado != EstadoPeriodo.ABIERTO) {
            throw new ReglaNegocioException("Este mes ya está cerrado y no se puede modificar.");
        }
    }

    public YearMonth periodo() {
        return periodo;
    }

    public int anio() {
        return periodo.getYear();
    }

    public int mes() {
        return periodo.getMonthValue();
    }

    /** Ventas registradas del mes, o cero si todavía no se registran. */
    public BigDecimal totalVentas() {
        return totalVentas == null ? Montos.CERO : totalVentas;
    }

    public boolean ventasRegistradas() {
        return totalVentas != null;
    }

    public EstadoPeriodo estado() {
        return estado;
    }

    public boolean estaAbierto() {
        return estado == EstadoPeriodo.ABIERTO;
    }

    /** Todas las facturas del mes, vigentes y anuladas, en el orden en que se registraron. */
    public List<FacturaCompra> facturas() {
        return Collections.unmodifiableList(facturas);
    }

    public List<FacturaCompra> facturasVigentes() {
        return facturas.stream().filter(FacturaCompra::esVigente).toList();
    }
}
