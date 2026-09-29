package pe.facturass20.dominio.puertos;

import java.util.List;
import java.util.Optional;

import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.Emisor;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.ImagenFactura;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;

/**
 * Facturas de compra guardadas. Las facturas de un mes se leen con {@link PeriodoRepositorio#buscar}.
 * Las escrituras son transaccionales: o se guarda todo o nada (RNF-13).
 */
public interface FacturaRepositorio {

    Optional<FacturaCompra> buscarPorId(long id);

    /** Todas las facturas (vigentes y anuladas) de ese emisor y serie, de cualquier mes; para detectar duplicados. */
    List<FacturaCompra> buscarPorEmisorYSerie(String rucEmisor, String serie);

    /** Para autocompletar la razón social en el registro manual (P19). */
    Optional<Emisor> buscarEmisor(String ruc);

    /**
     * En una sola transacción: crea el mes si no existe, guarda o actualiza el emisor, inserta la factura,
     * su imagen y sus campos leídos, y reemplaza la determinación del mes.
     *
     * @param imagen     nula si se registró a mano sin foto
     * @param extraccion nula si se registró a mano
     * @return el id de la factura nueva
     */
    long registrar(PeriodoMensual periodo, FacturaCompra factura, ImagenFactura imagen,
            ResultadoExtraccion extraccion, Determinacion determinacion);

    /** En una sola transacción: guarda el estado y el motivo de la factura anulada y la determinación recalculada. */
    void anular(FacturaCompra facturaAnulada, Determinacion determinacion);
}
