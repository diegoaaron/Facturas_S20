package pe.facturass20.datos.entidades;

import androidx.room.TypeConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.facturass20.dominio.reglas.Montos;

/**
 * Conversiones entre los tipos de Java y las columnas de SQLite (documentación técnica §7.1):
 * <ul>
 *   <li>{@link BigDecimal} ↔ {@code INTEGER} en céntimos ({@code 4120.50 → 412050}), porque SQLite guarda
 *       los decimales como {@code REAL} y redondearía. El umbral de aviso usa la misma regla: {@code 80 = 0,80}.</li>
 *   <li>{@link LocalDate} y {@link LocalDateTime} ↔ {@code TEXT} ISO-8601 ({@code 2026-09-22},
 *       {@code 2026-09-22T10:15:30}).</li>
 * </ul>
 * Los enums del dominio se guardan por su nombre ({@code VIGENTE}), con el conversor que trae Room.
 */
public final class Convertidores {

    private Convertidores() { }

    @TypeConverter
    public static Long aCentimos(BigDecimal monto) {
        return monto == null ? null : Montos.aCentimos(monto);
    }

    @TypeConverter
    public static BigDecimal desdeCentimos(Long centimos) {
        return centimos == null ? null : Montos.desdeCentimos(centimos);
    }

    @TypeConverter
    public static String aTexto(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }

    @TypeConverter
    public static LocalDate aFecha(String texto) {
        return texto == null ? null : LocalDate.parse(texto);
    }

    @TypeConverter
    public static String aTexto(LocalDateTime fechaHora) {
        return fechaHora == null ? null : fechaHora.toString();
    }

    @TypeConverter
    public static LocalDateTime aFechaHora(String texto) {
        return texto == null ? null : LocalDateTime.parse(texto);
    }
}
