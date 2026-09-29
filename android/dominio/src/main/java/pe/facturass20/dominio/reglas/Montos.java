package pe.facturass20.dominio.reglas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

/**
 * Dinero con {@link BigDecimal} de escala 2. Los montos se comparan con {@code compareTo}, nunca con
 * {@code equals} ({@code 5000.0} y {@code 5000.00} son el mismo monto).
 */
public final class Montos {

    public static final int ESCALA = 2;
    public static final BigDecimal CERO = BigDecimal.ZERO.setScale(ESCALA);

    private Montos() { }

    /** {@code Montos.de("1450.00")}: texto con punto decimal, como en el JSON del modelo o en los CSV. */
    public static BigDecimal de(String texto) {
        return normalizar(new BigDecimal(texto));
    }

    /** Lleva el monto a escala 2 redondeando al céntimo más cercano. */
    public static BigDecimal normalizar(BigDecimal monto) {
        return Objects.requireNonNull(monto, "monto").setScale(ESCALA, RoundingMode.HALF_UP);
    }

    public static boolean esPositivo(BigDecimal monto) {
        return monto != null && monto.signum() > 0;
    }

    public static BigDecimal mayor(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    /** Para guardar en SQLite como {@code INTEGER}: {@code 4120.50 → 412050}. */
    public static long aCentimos(BigDecimal monto) {
        return normalizar(monto).movePointRight(ESCALA).longValueExact();
    }

    public static BigDecimal desdeCentimos(long centimos) {
        return BigDecimal.valueOf(centimos, ESCALA);
    }

    /**
     * Interpreta un importe escrito por una persona o leído de la foto: {@code "1 450,00"},
     * {@code "1,450.00"}, {@code "1.450,00"}, {@code "S/ 1450"} y {@code "1450"} dan {@code 1450.00}.
     *
     * <p>Si hay coma y punto, el último es el separador decimal. Si hay uno solo, aparece una vez y le
     * siguen 1 o 2 cifras, es decimal ({@code "1450,5"}); con 3 cifras se toma como miles ({@code "1,450"}).
     * Vacío si el texto no es un monto, es negativo o tiene más de 2 decimales.</p>
     */
    public static Optional<BigDecimal> interpretar(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String t = texto.replace("S/", "").replaceAll("[\\s\\u00A0]", "");
        if (!t.matches("[0-9.,]*[0-9][0-9.,]*")) {
            return Optional.empty();
        }
        int coma = t.lastIndexOf(',');
        int punto = t.lastIndexOf('.');
        int separador = -1;
        if (coma >= 0 && punto >= 0) {
            separador = Math.max(coma, punto);
        } else if (coma >= 0 || punto >= 0) {
            int posicion = Math.max(coma, punto);
            boolean unico = t.indexOf(t.charAt(posicion)) == posicion;
            int cifrasDespues = t.length() - posicion - 1;
            if (unico && cifrasDespues != 3) {
                separador = posicion;
            }
        }
        String entera = (separador >= 0 ? t.substring(0, separador) : t).replaceAll("[.,]", "");
        String decimales = separador >= 0 ? t.substring(separador + 1) : "";
        if (decimales.length() > ESCALA || !decimales.matches("\\d*")) {
            return Optional.empty();
        }
        if (entera.isEmpty()) {
            entera = "0";
        }
        return Optional.of(normalizar(new BigDecimal(decimales.isEmpty() ? entera : entera + "." + decimales)));
    }
}
