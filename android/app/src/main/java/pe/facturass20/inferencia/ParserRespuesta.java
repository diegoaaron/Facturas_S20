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
import pe.facturass20.dominio.reglas.ValidadorCampos;
import pe.facturass20.dominio.reglas.ValidadorFecha;

/**
 * La instrucción que recibe el modelo y la lectura de su respuesta (documentación técnica §6.2 y §6.3).
 * La misma instrucción y el mismo esquema se usan en el cuaderno de ajuste fino.
 *
 * <p>El modelo responde un JSON plano, sin confianza ({@code {"ruc":"20601234565","serie":"F001",...}}),
 * porque escribir menos tokens acelera la lectura (decidido el 2026-10-25, §6.7). La confianza la pone la
 * app según el valor pase o no la validación del dominio. Si el modelo trae el esquema anterior, con
 * {@code valor} y {@code confianza} por campo, se usa la confianza que dio.</p>
 *
 * <p>Los valores quedan normalizados como los guarda la app: RUC solo dígitos, serie en mayúsculas, fecha
 * {@code 2026-09-22}, moneda {@code PEN} o {@code USD} e importe {@code 1450.00}. Un campo que falta o no
 * se entiende queda con valor nulo y confianza 0, para que el usuario lo complete en P08.</p>
 */
public final class ParserRespuesta {

    public static final String INSTRUCCION =
            "Extrae de esta factura peruana los campos ruc, razon_social, serie, numero, "
            + "fecha_emision (YYYY-MM-DD), moneda (PEN o USD) e importe_total (con punto decimal y dos "
            + "decimales, incluida la percepción si la hay). Responde solo un objeto JSON plano, sin espacios, "
            + "con los valores como texto. Si un campo no se lee, usa null.";

    /** Para el usuario cuando la respuesta no trae un JSON (§6.3). */
    static final String SIN_JSON = "No pudimos leer la factura. Tome otra foto con más luz.";
    /** Si el modelo no da la confianza y el valor pasa la validación: verde, sin pedir revisión. */
    static final double CONFIANZA_VALIDADO = 0.9;
    /**
     * Si el modelo no da la confianza y el valor no pasa la validación: queda por debajo de 0,80 para que
     * se muestre en ámbar (o en rojo, por la validación) y el usuario lo revise.
     */
    static final double CONFIANZA_SIN_VALIDAR = 0.5;

    /** El valor ya normalizado de un campo y la confianza que dio el modelo, si la dio. */
    private record Lectura(String valor, Optional<Double> confianzaDada) { }

    private ParserRespuesta() { }

    /** Los 7 campos, en el orden de {@link CampoFactura}. */
    public static List<CampoExtraido> interpretar(String respuesta) throws ExtraccionException {
        JsonObject json = primerObjeto(respuesta)
                .orElseThrow(() -> new ExtraccionException(SIN_JSON));
        List<Lectura> lecturas = new ArrayList<>();
        for (CampoFactura campo : CampoFactura.values()) {
            lecturas.add(leer(json, campo));
        }
        separarSerieDelNumero(lecturas);
        List<CampoExtraido> campos = new ArrayList<>();
        for (CampoFactura campo : CampoFactura.values()) {
            campos.add(conConfianza(campo, lecturas.get(campo.ordinal())));
        }
        return campos;
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

    private static Lectura leer(JsonObject json, CampoFactura campo) {
        JsonElement elemento = json.get(campo.clave());
        String texto;
        JsonElement confianzaDada = null;
        if (elemento != null && elemento.isJsonObject()) {
            // Esquema anterior: {"ruc": {"valor": "20601234565", "confianza": 0.98}}.
            JsonObject objeto = elemento.getAsJsonObject();
            texto = texto(objeto.get("valor"));
            confianzaDada = objeto.get("confianza");
        } else {
            texto = texto(elemento);
        }
        return new Lectura(texto == null ? null : normalizar(campo, texto), confianza(confianzaDada));
    }

    /** Sin valor, confianza 0; si el modelo no dio la confianza, se deduce de la validación del dominio. */
    private static CampoExtraido conConfianza(CampoFactura campo, Lectura lectura) {
        String valor = lectura.valor();
        if (valor == null) {
            return CampoExtraido.leido(campo, null, 0);
        }
        double confianza = lectura.confianzaDada()
                .orElseGet(() -> ValidadorCampos.error(campo, valor).isPresent()
                        ? CONFIANZA_SIN_VALIDAR : CONFIANZA_VALIDADO);
        return CampoExtraido.leido(campo, valor, confianza);
    }

    private static String texto(JsonElement elemento) {
        if (elemento == null || !elemento.isJsonPrimitive()) {
            return null;
        }
        String texto = elemento.getAsString().trim();
        return texto.isEmpty() || texto.equalsIgnoreCase("null") ? null : texto;
    }

    /** La confianza que dio el modelo, entre 0 y 1; vacío si no la dio o no es un número. */
    private static Optional<Double> confianza(JsonElement elemento) {
        if (elemento == null || !elemento.isJsonPrimitive()) {
            return Optional.empty();
        }
        JsonPrimitive primitivo = elemento.getAsJsonPrimitive();
        try {
            double valor = primitivo.isNumber() ? primitivo.getAsDouble()
                    : Double.parseDouble(primitivo.getAsString().trim().replace(',', '.'));
            return Double.isNaN(valor) ? Optional.empty() : Optional.of(Math.max(0, Math.min(1, valor)));
        } catch (NumberFormatException e) {
            return Optional.empty();
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
    private static void separarSerieDelNumero(List<Lectura> lecturas) {
        Lectura serie = lecturas.get(CampoFactura.SERIE.ordinal());
        Lectura numero = lecturas.get(CampoFactura.NUMERO.ordinal());
        String valor = numero.valor();
        if (valor == null || !valor.matches("[A-Z0-9]{4}-\\d+")) {
            return;
        }
        String[] partes = valor.split("-");
        if (serie.valor() != null && !serie.valor().equals(partes[0])) {
            return;
        }
        Optional<Double> confianzaSerie = serie.valor() == null ? numero.confianzaDada() : serie.confianzaDada();
        lecturas.set(CampoFactura.SERIE.ordinal(), new Lectura(partes[0], confianzaSerie));
        lecturas.set(CampoFactura.NUMERO.ordinal(), new Lectura(partes[1], numero.confianzaDada()));
    }
}
