package pe.facturass20.datos.entidades;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

/** Tabla {@code cronograma_venc}: fecha límite por año, mes y último dígito del RUC. */
@Entity(tableName = "cronograma_venc",
        foreignKeys = @ForeignKey(entity = ParametroVersionEntity.class, parentColumns = "id_parametro",
                childColumns = "id_parametro"),
        indices = @Index(value = {"id_parametro", "anio", "mes", "ultimo_digito"}, unique = true))
public class CronogramaVencEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_cronograma")
    public long idCronograma;

    @ColumnInfo(name = "id_parametro")
    public long idParametro;

    public int anio;

    public int mes;

    @ColumnInfo(name = "ultimo_digito")
    public int ultimoDigito;

    @NonNull
    @ColumnInfo(name = "fecha_limite")
    public LocalDate fechaLimite = LocalDate.MIN;
}
