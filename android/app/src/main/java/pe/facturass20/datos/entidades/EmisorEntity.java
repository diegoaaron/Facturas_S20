package pe.facturass20.datos.entidades;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Tabla {@code emisor}: proveedores de las facturas de compra. */
@Entity(tableName = "emisor", indices = @Index(value = "ruc", unique = true))
public class EmisorEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_emisor")
    public long idEmisor;

    @NonNull
    public String ruc = "";

    @NonNull
    @ColumnInfo(name = "razon_social")
    public String razonSocial = "";
}
