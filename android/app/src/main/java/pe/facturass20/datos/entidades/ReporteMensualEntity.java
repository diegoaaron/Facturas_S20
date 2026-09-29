package pe.facturass20.datos.entidades;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;

/** Tabla {@code reporte_mensual}: PDF generados de un mes (se usan desde la iteración I5). */
@Entity(tableName = "reporte_mensual",
        foreignKeys = @ForeignKey(entity = PeriodoEntity.class, parentColumns = "id_periodo",
                childColumns = "id_periodo"),
        indices = @Index("id_periodo"))
public class ReporteMensualEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_reporte")
    public long idReporte;

    @ColumnInfo(name = "id_periodo")
    public long idPeriodo;

    @NonNull
    @ColumnInfo(name = "ruta_pdf")
    public String rutaPdf = "";

    @NonNull
    public String sha256 = "";

    @NonNull
    @ColumnInfo(name = "fecha_generacion")
    public LocalDateTime fechaGeneracion = LocalDateTime.MIN;
}
