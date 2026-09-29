package pe.facturass20.dominio.reglas;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import pe.facturass20.dominio.modelo.FacturaCompra;

/**
 * Una factura es duplicada si ya hay otra VIGENTE con el mismo RUC de emisor, serie y número
 * (documentación técnica §5.5). Las anuladas no cuentan: la factura se puede volver a registrar.
 */
public final class DetectorDuplicados {

    private DetectorDuplicados() { }

    /** La factura ya registrada que choca con la candidata, si existe. */
    public static Optional<FacturaCompra> buscarDuplicado(FacturaCompra candidata,
            Collection<FacturaCompra> existentes) {
        String clave = candidata.claveUnica();
        return existentes.stream()
                .filter(FacturaCompra::esVigente)
                .filter(otra -> candidata.id() == null || !Objects.equals(otra.id(), candidata.id()))
                .filter(otra -> otra.claveUnica().equals(clave))
                .findFirst();
    }
}
