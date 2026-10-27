package pe.facturass20.exportacion;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.Test;

/** Formato de los CSV (documentación técnica §9.3). */
public class EscritorCsvTest {

    private static String escribir(Object... valores) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        EscritorCsv csv = new EscritorCsv(salida);
        csv.fila(valores);
        csv.terminar();
        byte[] bytes = salida.toByteArray();
        assertArrayEquals(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, Arrays.copyOf(bytes, 3));
        return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
    }

    @Test
    public void separaConPuntoYComaYTerminaEnCrLf() throws IOException {
        assertEquals("2026;9;PEN\r\n", escribir(2026, 9, "PEN"));
    }

    @Test
    public void montosConPuntoYDosDecimales() throws IOException {
        assertEquals("1450.00;0.50;1000000.00\r\n",
                escribir(new BigDecimal("1450"), new BigDecimal("0.5"), new BigDecimal("1E+6")));
    }

    @Test
    public void fechasEnIso() throws IOException {
        assertEquals("2026-09-22;2026-09-22T10:41:05\r\n",
                escribir(LocalDate.of(2026, 9, 22), LocalDateTime.of(2026, 9, 22, 10, 41, 5, 123_000_000)));
        assertEquals("2026-09-22T10:41:00\r\n", escribir(LocalDateTime.of(2026, 9, 22, 10, 41)));
    }

    @Test
    public void comillasSoloCuandoHacenFalta() throws IOException {
        assertEquals("\"A;B\";\"Dice \"\"hola\"\"\";\"dos\nlíneas\";simple\r\n",
                escribir("A;B", "Dice \"hola\"", "dos\nlíneas", "simple"));
    }

    @Test
    public void nuloVacioYTildesEnUtf8() throws IOException {
        assertEquals(";PANADERÍA ÑAÑA S.A.C.;\r\n", escribir(null, "PANADERÍA ÑAÑA S.A.C.", null));
    }
}
