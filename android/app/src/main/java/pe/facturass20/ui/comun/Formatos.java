package pe.facturass20.ui.comun;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Formatos de moneda y fecha que usa toda la interfaz (documentación técnica §8.3):
 * {@code S/ 4 120,50}, {@code 22/09/2026} y «jueves 15 de octubre».
 *
 * <p>Los nombres de días y meses están fijos para que el texto no dependa del idioma ni de la
 * versión de Android del teléfono. Los espacios son de no separación, para que un monto nunca se
 * parta en dos líneas.</p>
 */
public final class Formatos {

    /** Espacio de no separación: entre «S/» y el número, y como separador de miles. */
    public static final char ESPACIO = ' ';

    private static final String[] MESES = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
    private static final String[] DIAS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};

    private Formatos() { }

    /** {@code 4120.5 → "S/ 4 120,50"}; siempre con dos decimales. */
    public static String moneda(BigDecimal monto) {
        return formatear(monto, true);
    }

    /** Como {@link #moneda} pero sin «,00» cuando el monto es entero: {@code 5000 → "S/ 5 000"}. */
    public static String monedaCorta(BigDecimal monto) {
        boolean entero = monto.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().scale() <= 0;
        return formatear(monto, !entero);
    }

    /** {@code "22/09/2026"} */
    public static String fecha(LocalDate fecha) {
        return String.format("%02d/%02d/%04d", fecha.getDayOfMonth(), fecha.getMonthValue(), fecha.getYear());
    }

    /** {@code "22/09"}, para las listas del mes. */
    public static String diaMes(LocalDate fecha) {
        return String.format("%02d/%02d", fecha.getDayOfMonth(), fecha.getMonthValue());
    }

    /** {@code "jueves 15 de octubre"} */
    public static String fechaLarga(LocalDate fecha) {
        return DIAS[fecha.getDayOfWeek().getValue() - 1] + " " + fecha.getDayOfMonth() + " de "
                + MESES[fecha.getMonthValue() - 1];
    }

    /** {@code "Septiembre 2026"}, para el encabezado del período. */
    public static String periodo(YearMonth periodo) {
        String mes = MESES[periodo.getMonthValue() - 1];
        return Character.toUpperCase(mes.charAt(0)) + mes.substring(1) + " " + periodo.getYear();
    }

    private static String formatear(BigDecimal monto, boolean conDecimales) {
        BigDecimal redondeado = monto.setScale(2, RoundingMode.HALF_UP);
        String digitos = redondeado.abs().toPlainString();
        int punto = digitos.indexOf('.');
        String entero = digitos.substring(0, punto);
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < entero.length(); i++) {
            if (i > 0 && (entero.length() - i) % 3 == 0) {
                texto.append(ESPACIO);
            }
            texto.append(entero.charAt(i));
        }
        if (conDecimales) {
            texto.append(',').append(digitos.substring(punto + 1));
        }
        return (redondeado.signum() < 0 ? "-" : "") + "S/" + ESPACIO + texto;
    }
}
