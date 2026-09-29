package pe.facturass20.datos.dao;

import androidx.room.Embedded;
import androidx.room.Relation;

import pe.facturass20.datos.entidades.EmisorEntity;
import pe.facturass20.datos.entidades.FacturaCompraEntity;

/** Una factura con su emisor, para reconstruir la {@code FacturaCompra} del dominio. */
public class FacturaConEmisor {

    @Embedded
    public FacturaCompraEntity factura;

    @Relation(parentColumn = "id_emisor", entityColumn = "id_emisor")
    public EmisorEntity emisor;
}
