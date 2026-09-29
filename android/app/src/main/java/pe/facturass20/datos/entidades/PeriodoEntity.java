package pe.facturass20.datos.entidades;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;

import pe.facturass20.dominio.modelo.EstadoPeriodo;

/** Tabla {@code periodo}: un mes del contribuyente. */
@Entity(tableName = "periodo",
        foreignKeys = @ForeignKey(entity = ContribuyenteEntity.class, parentColumns = "id_contribuyente",
                childColumns = "id_contribuyente"),
        indices = @Index(value = {"id_contribuyente", "anio", "mes"}, unique = true))
public class PeriodoEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_periodo")
    public long idPeriodo;

    @ColumnInfo(name = "id_contribuyente")
    public long idContribuyente;

    public int anio;

    /** 1 a 12. */
    public int mes;

    /** En céntimos; nulo mientras no se registran las ventas del mes. */
    @ColumnInfo(name = "total_ventas")
    public BigDecimal totalVentas;

    @NonNull
    public EstadoPeriodo estado = EstadoPeriodo.ABIERTO;
}
