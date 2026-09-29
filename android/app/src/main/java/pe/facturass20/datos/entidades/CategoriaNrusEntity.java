package pe.facturass20.datos.entidades;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;

/** Tabla {@code categoria_nrus}: categorías de una versión de parámetros. */
@Entity(tableName = "categoria_nrus",
        foreignKeys = @ForeignKey(entity = ParametroVersionEntity.class, parentColumns = "id_parametro",
                childColumns = "id_parametro"),
        indices = @Index(value = {"id_parametro", "codigo"}, unique = true))
public class CategoriaNrusEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_categoria")
    public long idCategoria;

    @ColumnInfo(name = "id_parametro")
    public long idParametro;

    public int codigo;

    @NonNull
    @ColumnInfo(name = "limite_mensual")
    public BigDecimal limiteMensual = BigDecimal.ZERO;

    @NonNull
    public BigDecimal cuota = BigDecimal.ZERO;
}
