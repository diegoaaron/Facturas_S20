package pe.facturass20.ui.mes;

import java.math.BigDecimal;

import pe.facturass20.ui.comun.Formatos;

/**
 * Lo que se va escribiendo con el teclado de montos de P13: dígitos, una coma y hasta dos decimales.
 * Es Java puro para probarlo en la JVM.
 */
public final class EntradaMonto {

    /** Hasta 99 999 999,99: más que cualquier venta del NRUS. */
    static final int MAXIMO_ENTEROS = 8;

    private final StringBuilder texto = new StringBuilder();

    /** Empieza con un monto ya guardado ({@code 3900.00 → "3900"}, {@code 12.5 → "12,5"}). */
    public void cargar(BigDecimal monto) {
        texto.setLength(0);
        if (monto != null) {
            texto.append(monto.stripTrailingZeros().toPlainString().replace('.', ','));
            if (texto.toString().equals("0")) {
                texto.setLength(0);
            }
        }
    }

    public void digito(char digito) {
        if (digito < '0' || digito > '9') {
            throw new IllegalArgumentException("No es un dígito: " + digito);
        }
        int coma = texto.indexOf(",");
        if (coma >= 0) {
            if (texto.length() - coma - 1 >= 2) {
                return;
            }
        } else if (texto.length() >= MAXIMO_ENTEROS) {
            return;
        } else if (texto.toString().equals("0")) {
            texto.setLength(0);
        }
        texto.append(digito);
    }

    public void coma() {
        if (texto.indexOf(",") >= 0) {
            return;
        }
        if (texto.length() == 0) {
            texto.append('0');
        }
        texto.append(',');
    }

    public void borrar() {
        if (texto.length() > 0) {
            texto.setLength(texto.length() - 1);
        }
    }

    /** Lo escrito, p. ej. {@code "3900,5"}; vacío si no se escribió nada. */
    public String texto() {
        return texto.toString();
    }

    /** Como {@link #texto()} pero con el espacio de miles de §8.3: {@code "3900,5" → "3 900,5"}. */
    public String paraMostrar() {
        String escrito = texto.toString();
        int coma = escrito.indexOf(',');
        String entero = coma >= 0 ? escrito.substring(0, coma) : escrito;
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < entero.length(); i++) {
            if (i > 0 && (entero.length() - i) % 3 == 0) {
                resultado.append(Formatos.ESPACIO);
            }
            resultado.append(entero.charAt(i));
        }
        return coma >= 0 ? resultado + escrito.substring(coma) : resultado.toString();
    }

    public boolean vacio() {
        return texto.length() == 0;
    }

    /** El monto escrito, con dos decimales; cero si está vacío. */
    public BigDecimal monto() {
        if (texto.length() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        String numero = texto.toString().replace(',', '.');
        if (numero.endsWith(".")) {
            numero = numero.substring(0, numero.length() - 1);
        }
        return new BigDecimal(numero).setScale(2);
    }
}
