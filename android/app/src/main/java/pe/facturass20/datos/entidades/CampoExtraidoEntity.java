package pe.facturass20.datos.entidades;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import pe.facturass20.dominio.modelo.CampoFactura;

/**
 * Tabla {@code campo_extraido}: lo que leyó el modelo y lo que dejó el usuario, campo por campo. Sirve
 * para medir la exactitud real del modelo (RNF-01/02).
 */
@Entity(tableName = "campo_extraido",
        foreignKeys = {
                @ForeignKey(entity = FacturaCompraEntity.class, parentColumns = "id_factura",
                        childColumns = "id_factura"),
                @ForeignKey(entity = ModeloLocalEntity.class, parentColumns = "id_modelo", childColumns = "id_modelo")},
        indices = {@Index("id_factura"), @Index("id_modelo")})
public class CampoExtraidoEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_campo")
    public long idCampo;

    @ColumnInfo(name = "id_factura")
    public long idFactura;

    /** Nulo si la versión del modelo no está registrada en {@code modelo_local} (p. ej. el extractor falso). */
    @ColumnInfo(name = "id_modelo")
    public Long idModelo;

    @NonNull
    @ColumnInfo(name = "nombre_campo")
    public CampoFactura nombreCampo = CampoFactura.RUC;

    @ColumnInfo(name = "valor_leido")
    public String valorLeido;

    @ColumnInfo(name = "valor_final")
    public String valorFinal;

    public double confianza;

    public boolean corregido;
}
