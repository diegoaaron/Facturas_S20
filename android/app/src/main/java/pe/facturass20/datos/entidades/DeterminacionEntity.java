package pe.facturass20.datos.entidades;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.facturass20.dominio.modelo.NivelAlerta;

/**
 * Tabla {@code determinacion}: la última determinación de cada mes (una por período). Respecto del DER
 * se agregan {@code fecha_calculo} y {@code aviso_tope_anual}.
 */
@Entity(tableName = "determinacion",
        foreignKeys = {
                @ForeignKey(entity = PeriodoEntity.class, parentColumns = "id_periodo", childColumns = "id_periodo"),
                @ForeignKey(entity = CategoriaNrusEntity.class, parentColumns = "id_categoria",
                        childColumns = "id_categoria")},
        indices = {@Index(value = "id_periodo", unique = true), @Index("id_categoria")})
public class DeterminacionEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_determinacion")
    public long idDeterminacion;

    @ColumnInfo(name = "id_periodo")
    public long idPeriodo;

    /** Nulo si el mes quedó fuera del régimen. */
    @ColumnInfo(name = "id_categoria")
    public Long idCategoria;

    @NonNull
    @ColumnInfo(name = "total_adquisiciones")
    public BigDecimal totalAdquisiciones = BigDecimal.ZERO;

    @NonNull
    @ColumnInfo(name = "monto_determinante")
    public BigDecimal montoDeterminante = BigDecimal.ZERO;

    /** Nula si el mes quedó fuera del régimen. */
    public BigDecimal cuota;

    @NonNull
    @ColumnInfo(name = "fecha_vencimiento")
    public LocalDate fechaVencimiento = LocalDate.MIN;

    @NonNull
    @ColumnInfo(name = "nivel_alerta")
    public NivelAlerta nivelAlerta = NivelAlerta.NINGUNA;

    @ColumnInfo(name = "aviso_tope_anual")
    public boolean avisoTopeAnual;

    @NonNull
    @ColumnInfo(name = "fecha_calculo")
    public LocalDateTime fechaCalculo = LocalDateTime.MIN;
}
