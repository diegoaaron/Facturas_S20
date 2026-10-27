package pe.facturass20.exportacion;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CSV para Excel y LibreOffice (documentación técnica §9.3): UTF-8 con BOM, separador {@code ;}, fin de
 * línea {@code \r\n}, comillas solo cuando hacen falta (RFC 4180), montos con punto y dos decimales y
 * fechas ISO. No cierra el flujo: escribe dentro de una entrada del ZIP.
 */
public final class EscritorCsv {

    private static final char SEPARADOR = ';';
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final Writer salida;

    public EscritorCsv(OutputStream destino) throws IOException {
        destino.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});   // BOM para Excel
        this.salida = new OutputStreamWriter(destino, StandardCharsets.UTF_8);
    }

    /** Una fila; {@code null} queda vacío. */
    public void fila(Object... valores) throws IOException {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                salida.write(SEPARADOR);
            }
            salida.write(formatear(valores[i]));
        }
        salida.write("\r\n");
    }

    /** Pasa lo escrito al flujo; hay que llamarlo antes de cerrar la entrada del ZIP. */
    public void terminar() throws IOException {
        salida.flush();
    }

    static String formatear(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto;
        if (valor instanceof BigDecimal monto) {
            texto = monto.setScale(2, RoundingMode.HALF_UP).toPlainString();   // 1450.00
        } else if (valor instanceof LocalDateTime fechaHora) {
            texto = FECHA_HORA.format(fechaHora);                              // 2026-09-22T10:41:05
        } else {
            texto = valor.toString();                                          // LocalDate → ISO
        }
        boolean requiereComillas = texto.indexOf(SEPARADOR) >= 0 || texto.indexOf('"') >= 0
                || texto.indexOf('\n') >= 0 || texto.indexOf('\r') >= 0;
        return requiereComillas ? '"' + texto.replace("\"", "\"\"") + '"' : texto;
    }
}
