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

import pe.facturass20.dominio.modelo.EstadoFactura;
import pe.facturass20.dominio.modelo.Moneda;
import pe.facturass20.dominio.modelo.OrigenRegistro;

/**
 * Tabla {@code factura_compra}.
 *
 * <p>A diferencia del DER del informe, <b>no hay UNIQUE(id_emisor, serie, numero)</b>: una factura
 * anulada se puede volver a registrar y {@code 004821} es la misma factura que {@code 4821}. El duplicado
 * lo detecta el dominio ({@code DetectorDuplicados}) contra las VIGENTE, usando {@code numero_normalizado}
 * (sin ceros de relleno), que tiene índice.</p>
 */
@Entity(tableName = "factura_compra",
        foreignKeys = {
                @ForeignKey(entity = PeriodoEntity.class, parentColumns = "id_periodo", childColumns = "id_periodo"),
                @ForeignKey(entity = EmisorEntity.class, parentColumns = "id_emisor", childColumns = "id_emisor")},
        indices = {
                @Index(value = {"id_periodo", "estado"}),
                @Index(value = {"id_emisor", "serie", "numero_normalizado"})})
public class FacturaCompraEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_factura")
    public long idFactura;

    @ColumnInfo(name = "id_periodo")
    public long idPeriodo;

    @ColumnInfo(name = "id_emisor")
    public long idEmisor;

    @NonNull
    public String serie = "";

    /** Tal como está impreso, p. ej. {@code 004821}. */
    @NonNull
    public String numero = "";

    /** Sin ceros de relleno, p. ej. {@code 4821}; para buscar duplicados. */
    @NonNull
    @ColumnInfo(name = "numero_normalizado")
    public String numeroNormalizado = "";

    @NonNull
    @ColumnInfo(name = "fecha_emision")
    public LocalDate fechaEmision = LocalDate.MIN;

    @NonNull
    public Moneda moneda = Moneda.PEN;

    /** En céntimos; siempre mayor que cero. */
    @NonNull
    @ColumnInfo(name = "importe_total")
    public BigDecimal importeTotal = BigDecimal.ZERO;

    @NonNull
    public OrigenRegistro origen = OrigenRegistro.IA;

    @NonNull
    public EstadoFactura estado = EstadoFactura.VIGENTE;

    @ColumnInfo(name = "motivo_anulacion")
    public String motivoAnulacion;

    @NonNull
    @ColumnInfo(name = "fecha_registro")
    public LocalDateTime fechaRegistro = LocalDateTime.MIN;
}
