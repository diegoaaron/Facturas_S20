package pe.facturass20.dominio.casosuso;

import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.ReglaNegocioException;

/** Ya hay una factura vigente con el mismo RUC de emisor, serie y número (P09, RF-07). */
public final class FacturaDuplicadaException extends ReglaNegocioException {

    private final transient FacturaCompra existente;

    public FacturaDuplicadaException(FacturaCompra existente) {
        super("Esta factura ya está registrada.");
        this.existente = existente;
    }

    /** La factura que ya estaba registrada, para ofrecer «Ver registrada». */
    public FacturaCompra existente() {
        return existente;
    }
}
