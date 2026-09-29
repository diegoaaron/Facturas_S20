package pe.facturass20.dominio.reglas;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Objects;
import java.util.Optional;

import pe.facturass20.dominio.modelo.EstadoPeriodo;

/**
 * Regla de la fecha de la factura (documentación técnica §5.5): debe ser del mes actual, o del mes
 * anterior mientras no esté cerrado; si no, se pide confirmar el período.
 */
public final class ValidadorFecha {

    /** Cómo encaja la fecha de emisión con los meses que se pueden registrar. */
    public enum Resultado {
        /** Del mes actual. */
        DEL_MES,
        /** Del mes anterior, que sigue abierto. */
        DEL_MES_ANTERIOR,
        /** De un mes más antiguo: se registra solo si el usuario confirma el período. */
        REQUIERE_CONFIRMACION,
        /** Su mes ya está cerrado: no se puede registrar. */
        PERIODO_CERRADO,
        /** Posterior a hoy: no se puede registrar. */
        FUTURA;

        public boolean permiteRegistrar() {
            return this != PERIODO_CERRADO && this != FUTURA;
        }
    }

    private static final DateTimeFormatter DIA_MES_ANIO =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private ValidadorFecha() { }

    /** Acepta {@code 2026-09-22} (como la devuelve el modelo) y {@code 22/09/2026} (como la escribe el usuario). */
    public static Optional<LocalDate> interpretar(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String t = texto.trim();
        try {
            return Optional.of(t.contains("/") ? LocalDate.parse(t, DIA_MES_ANIO) : LocalDate.parse(t));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    /**
     * @param estadoPeriodoFactura estado del mes de la factura, o nulo si ese mes todavía no existe
     */
    public static Resultado evaluar(LocalDate fechaEmision, LocalDate hoy, EstadoPeriodo estadoPeriodoFactura) {
        Objects.requireNonNull(fechaEmision, "fechaEmision");
        Objects.requireNonNull(hoy, "hoy");
        if (fechaEmision.isAfter(hoy)) {
            return Resultado.FUTURA;
        }
        if (estadoPeriodoFactura == EstadoPeriodo.CERRADO) {
            return Resultado.PERIODO_CERRADO;
        }
        YearMonth mesFactura = YearMonth.from(fechaEmision);
        YearMonth mesActual = YearMonth.from(hoy);
        if (mesFactura.equals(mesActual)) {
            return Resultado.DEL_MES;
        }
        if (mesFactura.equals(mesActual.minusMonths(1))) {
            return Resultado.DEL_MES_ANTERIOR;
        }
        return Resultado.REQUIERE_CONFIRMACION;
    }
}
