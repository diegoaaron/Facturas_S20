package pe.facturass20.datos.entidades;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;

/** Tabla {@code imagen_factura}: la foto cifrada de una factura (a lo más una por factura). */
@Entity(tableName = "imagen_factura",
        foreignKeys = @ForeignKey(entity = FacturaCompraEntity.class, parentColumns = "id_factura",
                childColumns = "id_factura"),
        indices = @Index(value = "id_factura", unique = true))
public class ImagenFacturaEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_imagen")
    public long idImagen;

    @ColumnInfo(name = "id_factura")
    public long idFactura;

    /** Nombre del archivo cifrado dentro de {@code filesDir/imagenes/}. */
    @NonNull
    @ColumnInfo(name = "ruta_cifrada")
    public String rutaCifrada = "";

    public double nitidez;

    @NonNull
    @ColumnInfo(name = "fecha_captura")
    public LocalDateTime fechaCaptura = LocalDateTime.MIN;
}
