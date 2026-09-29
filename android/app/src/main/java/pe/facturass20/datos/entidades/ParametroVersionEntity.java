package pe.facturass20.datos.entidades;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Tabla {@code parametro_version}: una fila por versión de {@code assets/parametros_nrus.json} (§5.2). */
@Entity(tableName = "parametro_version", indices = @Index(value = "version", unique = true))
public class ParametroVersionEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_parametro")
    public long idParametro;

    @NonNull
    public String version = "";

    /** En centésimas, como los montos: {@code 80} es 0,80. */
    @NonNull
    @ColumnInfo(name = "umbral_aviso")
    public BigDecimal umbralAviso = BigDecimal.ZERO;

    @NonNull
    @ColumnInfo(name = "tope_anual")
    public BigDecimal topeAnual = BigDecimal.ZERO;

    @NonNull
    @ColumnInfo(name = "vigente_desde")
    public LocalDate vigenteDesde = LocalDate.MIN;

    /** Solo una versión está activa: la última cargada. */
    public boolean activo;
}
