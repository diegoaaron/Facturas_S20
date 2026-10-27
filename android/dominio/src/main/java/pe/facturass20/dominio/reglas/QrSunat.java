package pe.facturass20.dominio.reglas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.DatosQr;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;

/**
 * El código QR de los comprobantes electrónicos: lo interpreta y corrige con él lo que leyó el modelo.
 *
 * <p>SUNAT pide {@code RUC|tipo|serie|número|IGV|total|fecha|tipo doc. adquirente|n.º doc.|...}, pero cada
 * emisor lo arma distinto (revisado en las fotos del dataset el 2026-10-27): serie y número juntos
 * ({@code B952-3956000}), la serie sin la letra ({@code 020-02517975}), la fecha como {@code 04/12/2025},
 * sin RUC, serie ni número ({@code |7.32|47,95|2025-12-01|1|...}) o en líneas {@code clave:valor}
 * ({@code NumFac:B101-59541}). Por eso la fecha se busca por su forma y el total es el campo anterior.
 * Lo demás (Yape, enlaces) no se toma como QR del comprobante.</p>
 */
public final class QrSunat {

    /** Lo que se lee del QR no es una estimación del modelo: se muestra en verde. */
    static final double CONFIANZA_QR = 1.0;
    /**
     * El QR trae el total sin la percepción. Si lo que leyó el modelo es mayor hasta en esta proporción
     * (la tasa general de percepción es 2 %), se toma como el total con percepción.
     */
    private static final BigDecimal PERCEPCION_MAXIMA = new BigDecimal("1.02");
    private static final BigDecimal UN_CENTIMO = new BigDecimal("0.01");
    private static final Pattern SERIE_Y_NUMERO = Pattern.compile("([A-Z0-9]{3,4})-(\\d{1,8})");

    private QrSunat() { }

    /** El primero de los textos que sea el QR de un comprobante (en una foto puede haber más de un QR). */
    public static Optional<DatosQr> primero(List<String> textos) {
        for (String texto : textos) {
            Optional<DatosQr> datos = interpretar(texto);
            if (datos.isPresent()) {
                return datos;
            }
        }
        return Optional.empty();
    }

    /** Vacío si el texto no es el QR de un comprobante: tiene que traer por lo menos la fecha y el total. */
    public static Optional<DatosQr> interpretar(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String t = texto.trim().toUpperCase(Locale.ROOT);
        DatosQr datos;
        if (t.contains("|")) {
            datos = separadoPorBarras(t);
        } else if (t.contains("NUMFAC:")) {
            datos = claveValor(t);
        } else {
            return Optional.empty();
        }
        return datos != null && datos.fechaEmision() != null && datos.importeTotal() != null
                ? Optional.of(datos) : Optional.empty();
    }

    private static DatosQr separadoPorBarras(String texto) {
        String[] c = texto.split("\\|", -1);
        for (int i = 0; i < c.length; i++) {
            c[i] = c[i].trim();
        }
        int iFecha = -1;
        for (int i = 1; i < c.length && iFecha < 0; i++) {
            if (ValidadorFecha.interpretar(c[i]).isPresent()) {
                iFecha = i;
            }
        }
        if (iFecha < 0) {
            return null;
        }
        String ruc = ValidadorRuc.esValido(c[0]) ? c[0] : null;
        String tipo = c.length > 1 && c[1].matches("\\d{2}") ? c[1] : null;
        String serie = null;
        String numero = null;
        // Entre el tipo y el IGV (que va antes del total) queda el comprobante, en uno o dos campos.
        int iIgv = iFecha - 2;
        if (ruc != null && iIgv > 2) {
            Matcher junto = SERIE_Y_NUMERO.matcher(c[2]);
            if (junto.matches()) {
                serie = junto.group(1);
                numero = junto.group(2);
            } else if (iIgv > 3) {
                serie = c[2];
                numero = c[3];
            }
        }
        return armar(ruc, tipo, serie, numero, c[iFecha], c[iFecha - 1]);
    }

    /** Una línea {@code clave:valor} por dato: {@code NumFac}, {@code FecFac}, {@code NitFac}, {@code ValTolFac}. */
    private static DatosQr claveValor(String texto) {
        Map<String, String> valores = new HashMap<>();
        for (String linea : texto.split("\\R")) {
            int dosPuntos = linea.indexOf(':');
            if (dosPuntos > 0) {
                valores.put(linea.substring(0, dosPuntos).trim(), linea.substring(dosPuntos + 1).trim());
            }
        }
        String serie = null;
        String numero = null;
        Matcher comprobante = SERIE_Y_NUMERO.matcher(valores.getOrDefault("NUMFAC", ""));
        if (comprobante.matches()) {
            serie = comprobante.group(1);
            numero = comprobante.group(2);
        }
        String ruc = valores.get("NITFAC");
        return armar(ValidadorRuc.esValido(ruc) ? ruc : null, null, serie, numero,
                valores.get("FECFAC"), valores.get("VALTOLFAC"));
    }

    private static DatosQr armar(String ruc, String tipo, String serie, String numero, String fecha, String total) {
        String sinCeros = numero != null && numero.matches("\\d{1,8}") ? sinCeros(numero) : null;
        return new DatosQr(ruc, serie(serie, tipo), sinCeros, ValidadorFecha.interpretar(fecha).orElse(null),
                Montos.interpretar(total).filter(Montos::esPositivo).orElse(null));
    }

    /** La serie de 4 caracteres; si el emisor quitó la letra ({@code 020}), se pone la del tipo (01 → F, 03 → B). */
    private static String serie(String serie, String tipo) {
        if (serie == null) {
            return null;
        }
        if (serie.matches("[A-Z0-9]{4}")) {
            return serie;
        }
        if (serie.matches("\\d{3}") && "01".equals(tipo)) {
            return "F" + serie;
        }
        if (serie.matches("\\d{3}") && "03".equals(tipo)) {
            return "B" + serie;
        }
        return null;
    }

    private static String sinCeros(String numero) {
        return numero.replaceFirst("^0+(?=\\d)", "");
    }

    /**
     * Reemplaza lo que leyó el modelo por lo que trae el QR, que no tiene errores de lectura. Se conserva lo
     * del modelo cuando coincide en el número (con los ceros impresos) o cuando su total es el del QR más
     * una percepción posible. La razón social y la moneda quedan como las leyó el modelo.
     */
    public static ResultadoExtraccion completar(ResultadoExtraccion resultado, DatosQr qr) {
        List<CampoExtraido> campos = new ArrayList<>();
        for (CampoFactura nombre : CampoFactura.values()) {
            Optional<CampoExtraido> leido = resultado.campo(nombre);
            String delModelo = leido.map(CampoExtraido::valorLeido).orElse(null);
            String delQr = switch (nombre) {
                case RUC -> qr.ruc();
                case SERIE -> qr.serie();
                case NUMERO -> numero(delModelo, qr.numero());
                case FECHA_EMISION -> qr.fechaEmision() == null ? null : qr.fechaEmision().toString();
                case IMPORTE_TOTAL -> importe(delModelo, qr.importeTotal());
                case RAZON_SOCIAL, MONEDA -> null;
            };
            if (delQr != null) {
                campos.add(CampoExtraido.leido(nombre, delQr, CONFIANZA_QR));
            } else {
                leido.ifPresent(campos::add);
            }
        }
        return new ResultadoExtraccion(resultado.versionModelo(), resultado.tiempoMs(), campos);
    }

    private static String numero(String delModelo, String delQr) {
        if (delQr == null) {
            return null;
        }
        return delModelo != null && delModelo.matches("\\d{1,8}") && sinCeros(delModelo).equals(delQr)
                ? delModelo : delQr;
    }

    private static String importe(String delModelo, BigDecimal delQr) {
        if (delQr == null) {
            return null;
        }
        Optional<BigDecimal> modelo = Montos.interpretar(delModelo);
        BigDecimal maximo = delQr.multiply(PERCEPCION_MAXIMA).add(UN_CENTIMO);
        boolean conPercepcion = modelo.isPresent()
                && modelo.get().compareTo(delQr) >= 0 && modelo.get().compareTo(maximo) <= 0;
        return (conPercepcion ? modelo.get() : delQr).toPlainString();
    }
}
