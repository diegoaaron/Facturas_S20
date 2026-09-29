package pe.facturass20.datos.entidades;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;

import pe.facturass20.dominio.modelo.EstadoModelo;

/** Tabla {@code modelo_local}: versiones del modelo Gemma descargadas en el teléfono (§6.6). */
@Entity(tableName = "modelo_local", indices = @Index(value = "version", unique = true))
public class ModeloLocalEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_modelo")
    public long idModelo;

    @NonNull
    public String version = "";

    @ColumnInfo(name = "tamano_mb")
    public int tamanoMb;

    @NonNull
    public String sha256 = "";

    @NonNull
    public EstadoModelo estado = EstadoModelo.NO_INSTALADO;

    public String ruta;

    @ColumnInfo(name = "fecha_instalacion")
    public LocalDateTime fechaInstalacion;
}
