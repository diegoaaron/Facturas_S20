package pe.facturass20.datos.entidades;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

import pe.facturass20.dominio.modelo.TipoAviso;

/** Tabla {@code aviso}: notificaciones programadas de un mes (se usan desde la iteración I5). */
@Entity(tableName = "aviso",
        foreignKeys = @ForeignKey(entity = PeriodoEntity.class, parentColumns = "id_periodo",
                childColumns = "id_periodo"),
        indices = @Index("id_periodo"))
public class AvisoEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_aviso")
    public long idAviso;

    @ColumnInfo(name = "id_periodo")
    public long idPeriodo;

    @NonNull
    public TipoAviso tipo = TipoAviso.VENCIMIENTO;

    @NonNull
    @ColumnInfo(name = "fecha_programada")
    public LocalDate fechaProgramada = LocalDate.MIN;

    public boolean enviado;
}
