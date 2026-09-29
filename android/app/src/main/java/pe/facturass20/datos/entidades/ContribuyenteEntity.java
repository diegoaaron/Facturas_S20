package pe.facturass20.datos.entidades;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

/** Tabla {@code contribuyente}: el titular del negocio (una sola fila). */
@Entity(tableName = "contribuyente", indices = @Index(value = "ruc", unique = true))
public class ContribuyenteEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_contribuyente")
    public long idContribuyente;

    @NonNull
    public String ruc = "";

    @NonNull
    public String nombre = "";

    @NonNull
    public String titular = "";

    @ColumnInfo(name = "ultimo_digito")
    public int ultimoDigito;

    /** PBKDF2 del PIN en Base64; nulo hasta que se define el PIN en P01. */
    @ColumnInfo(name = "pin_hash")
    public String pinHash;

    @ColumnInfo(name = "pin_sal")
    public String pinSal;

    @NonNull
    @ColumnInfo(name = "fecha_alta")
    public LocalDate fechaAlta = LocalDate.MIN;
}
