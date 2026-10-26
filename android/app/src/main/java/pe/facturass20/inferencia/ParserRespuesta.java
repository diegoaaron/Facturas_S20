package pe.facturass20.inferencia;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.puertos.ExtraccionException;
import pe.facturass20.dominio.reglas.Montos;
import pe.facturass20.dominio.reglas.ValidadorFecha;

/**
 * La instrucción que recibe el modelo y la lectura de su respuesta (documentación técnica §6.2 y §6.3).
 * La misma instrucción y el mismo esquema se usan en el cuaderno de ajuste fino.
 *
 * <p>Los valores quedan normalizados como los guarda la app: RUC solo dígitos, serie en mayúsculas, fecha
 * {@code 2026-09-22}, moneda {@code PEN} o {@code USD} e importe {@code 1450.00}. Un campo que falta o no
 * se entiende queda con valor nulo y confianza 0, para que el usuario lo complete en P08.</p>
 */
public final class ParserRespuesta {

    public static final String INSTRUCCION =
            "Extrae de esta factura peruana los campos ruc, razon_social, serie, numero, "
            + "fecha_emision (YYYY-MM-DD), moneda (PEN o USD) e importe_total (numero con punto decimal). "
            + "Responde solo un objeto JSON donde cada campo tenga 'valor' y 'confianza' entre 0 y 1. "
            + "Si un campo no se lee, usa valor null y confianza 0.";

    /** Para el usuario cuando la respuesta no trae un JSON (§6.3). */
    static final String SIN_JSON = "No pudimos leer la factura. Tome otra foto con más luz.";
    /**
     * Si el modelo da el valor sin su confianza (el modelo base suele hacerlo): queda por debajo de 0,80
     * para que se muestre en ámbar y el usuario lo revise.
     */
    static final double CONFIANZA_SIN_DATO = 0.5;

    private ParserRespuesta() { }

    /** Los 7 campos, en el orden de {@link CampoFactura}. */
    public static List<CampoExtraido> interpretar(String respuesta) throws ExtraccionException {
        JsonObject json = primerObjeto(respuesta)
                .orElseThrow(() -> new ExtraccionException(SIN_JSON));
        List<CampoExtraido> campos = new ArrayList<>();
        for (CampoFactura campo : CampoFactura.values()) {
            campos.add(leer(json, campo));
        }
        return separarSerieDelNumero(campos);
    }

    /** El primer objeto JSON del texto, aunque venga dentro de {@code ```json} o con texto alrededor. */
    static Optional<JsonObject> primerObjeto(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        for (int inicio = texto.indexOf('{'); inicio >= 0; inicio = texto.indexOf('{', inicio + 1)) {
            int fin = cierre(texto, inicio);
            if (fin < 0) {
                return Optional.empty();
            }
            try {
                JsonElement elemento = JsonParser.parseString(texto.substring(inicio, fin + 1));
                if (elemento.isJsonObject()) {
                    return Optional.of(elemento.getAsJsonObject());
                }
            } catch (JsonParseException e) {
                // Sigue con la siguiente llave.
            }
        }
        return Optional.empty();
    }

    /**
     * Si el texto ya trae completo el primer objeto JSON. El puente lo usa para cortar la generación: lo que
     * el modelo escriba después no se usa y solo suma segundos.
     */
    public static boolean objetoCompleto(CharSequence texto) {
        String t = texto.toString();
        int inicio = t.indexOf('{');
        return inicio >= 0 && cierre(t, inicio) >= 0;
    }

    /** Posición de la llave que cierra la que abre en {@code inicio}, sin contar las que van entre comillas. */
    private static int cierre(String texto, int inicio) {
        int nivel = 0;
        boolean enTexto = false;
        for (int i = inicio; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (enTexto) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    enTexto = false;
                }
            } else if (c == '"') {
                enTexto = true;
            } else if (c == '{') {
                nivel++;
            } else if (c == '}' && --nivel == 0) {
                return i;
            }
        }
        return -1;
    }

    private static CampoExtraido leer(JsonObject json, CampoFactura campo) {
        JsonElement elemento = json.get(campo.clave());
        String texto;
        double confianza;
        if (elemento != null && elemento.isJsonObject()) {
            JsonObject objeto = elemento.getAsJsonObject();
            texto = texto(objeto.get("valor"));
            confianza = confianza(objeto.get("confianza"));
        } else {
            // Sin el esquema pedido: {"ruc": "20601234565"}.
            texto = texto(elemento);
            confianza = CONFIANZA_SIN_DATO;
        }
        String valor = texto == null ? null : normalizar(campo, texto);
        return CampoExtraido.leido(campo, valor, valor == null ? 0 : confianza);
    }

    private static String texto(JsonElement elemento) {
        if (elemento == null || !elemento.isJsonPrimitive()) {
            return null;
        }
        String texto = elemento.getAsString().trim();
        return texto.isEmpty() || texto.equalsIgnoreCase("null") ? null : texto;
    }

    private static double confianza(JsonElement elemento) {
        if (elemento == null || !elemento.isJsonPrimitive()) {
            return CONFIANZA_SIN_DATO;
        }
        JsonPrimitive primitivo = elemento.getAsJsonPrimitive();
        try {
            double valor = primitivo.isNumber() ? primitivo.getAsDouble()
                    : Double.parseDouble(primitivo.getAsString().trim().replace(',', '.'));
            return Double.isNaN(valor) ? CONFIANZA_SIN_DATO : Math.max(0, Math.min(1, valor));
        } catch (NumberFormatException e) {
            return CONFIANZA_SIN_DATO;
        }
    }

    /** Nulo si el valor no tiene sentido para el campo. */
    static String normalizar(CampoFactura campo, String texto) {
        switch (campo) {
            case RUC: {
                String digitos = texto.replaceAll("\\D", "");
                return digitos.isEmpty() ? null : digitos;
            }
            case RAZON_SOCIAL:
                return texto.replaceAll("\\s+", " ");
            case SERIE:
                return texto.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
            case NUMERO:
                return texto.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
            case FECHA_EMISION: {
                // También 22-09-2026 y 22.09.2026, que no son el formato pedido pero aparecen en las facturas.
                String fecha = texto.matches("\\d{1,2}[-.]\\d{1,2}[-.]\\d{4}") ? texto.replaceAll("[-.]", "/") : texto;
                return ValidadorFecha.interpretar(fecha).map(LocalDate::toString).orElse(null);
            }
            case MONEDA:
                return moneda(texto);
            case IMPORTE_TOTAL:
                return Montos.interpretar(texto).map(BigDecimal::toPlainString).orElse(null);
            default:
                throw new IllegalArgumentException(campo.name());
        }
    }

    private static String moneda(String texto) {
        String t = texto.toUpperCase(Locale.ROOT).replaceAll("[\\s.]", "");
        if (t.equals("PEN") || t.equals("S/") || t.startsWith("SOL")) {
            return "PEN";
        }
        if (t.equals("USD") || t.equals("$") || t.equals("US$") || t.startsWith("DOLAR") || t.startsWith("DÓLAR")) {
            return "USD";
        }
        return null;
    }

    /**
     * Si el modelo puso el comprobante entero en el número ({@code F001-004521}) y la serie falta o es la
     * misma, los separa.
     */
    private static List<CampoExtraido> separarSerieDelNumero(List<CampoExtraido> campos) {
        CampoExtraido serie = campos.get(CampoFactura.SERIE.ordinal());
        CampoExtraido numero = campos.get(CampoFactura.NUMERO.ordinal());
        String valor = numero.valorLeido();
        if (valor == null || !valor.matches("[A-Z0-9]{4}-\\d+")) {
            return campos;
        }
        String[] partes = valor.split("-");
        if (serie.valorLeido() != null && !serie.valorLeido().equals(partes[0])) {
            return campos;
        }
        double confianzaSerie = serie.valorLeido() == null ? numero.confianza() : serie.confianza();
        campos.set(CampoFactura.SERIE.ordinal(), CampoExtraido.leido(CampoFactura.SERIE, partes[0], confianzaSerie));
        campos.set(CampoFactura.NUMERO.ordinal(), CampoExtraido.leido(CampoFactura.NUMERO, partes[1], numero.confianza()));
        return campos;
    }
}
