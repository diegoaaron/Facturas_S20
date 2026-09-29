package pe.facturass20.datos.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import pe.facturass20.datos.entidades.CampoExtraidoEntity;
import pe.facturass20.datos.entidades.EmisorEntity;
import pe.facturass20.datos.entidades.FacturaCompraEntity;
import pe.facturass20.datos.entidades.ImagenFacturaEntity;

/** Facturas de compra con su emisor, su imagen y sus campos leídos. */
@Dao
public interface FacturaDao {

    @Transaction
    @Query("SELECT * FROM factura_compra WHERE id_periodo = :idPeriodo ORDER BY id_factura")
    List<FacturaConEmisor> listarPorPeriodo(long idPeriodo);

    @Transaction
    @Query("SELECT * FROM factura_compra WHERE id_factura = :idFactura")
    FacturaConEmisor buscarPorId(long idFactura);

    @Transaction
    @Query("SELECT f.* FROM factura_compra f JOIN emisor e ON e.id_emisor = f.id_emisor "
            + "WHERE e.ruc = :rucEmisor AND f.serie = :serie ORDER BY f.id_factura")
    List<FacturaConEmisor> buscarPorEmisorYSerie(String rucEmisor, String serie);

    @Insert
    long insertar(FacturaCompraEntity factura);

    /** {@code estado} es el nombre del enum, p. ej. {@code "ANULADA"}. */
    @Query("UPDATE factura_compra SET estado = :estado, motivo_anulacion = :motivo WHERE id_factura = :idFactura")
    int actualizarEstado(long idFactura, String estado, String motivo);

    @Query("SELECT * FROM emisor WHERE ruc = :ruc")
    EmisorEntity buscarEmisor(String ruc);

    @Insert
    long insertarEmisor(EmisorEntity emisor);

    @Update
    void actualizarEmisor(EmisorEntity emisor);

    @Insert
    long insertarImagen(ImagenFacturaEntity imagen);

    @Query("SELECT * FROM imagen_factura WHERE id_factura = :idFactura")
    ImagenFacturaEntity buscarImagen(long idFactura);

    @Insert
    void insertarCampos(List<CampoExtraidoEntity> campos);

    @Query("SELECT * FROM campo_extraido WHERE id_factura = :idFactura ORDER BY id_campo")
    List<CampoExtraidoEntity> listarCampos(long idFactura);
}
