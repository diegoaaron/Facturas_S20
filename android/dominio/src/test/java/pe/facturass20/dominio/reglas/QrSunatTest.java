package pe.facturass20.dominio.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.DatosQr;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;

/** Los textos son QR de las fotos del dataset, con el documento del cliente cambiado. */
class QrSunatTest {

    private static DatosQr leer(String texto) {
        return QrSunat.interpretar(texto).orElseThrow();
    }

    private static void assertDatos(DatosQr datos, String ruc, String serie, String numero, String fecha,
                                    String total) {
        assertEquals(ruc, datos.ruc());
        assertEquals(serie, datos.serie());
        assertEquals(numero, datos.numero());
        assertEquals(LocalDate.parse(fecha), datos.fechaEmision());
        assertEquals(new BigDecimal(total), datos.importeTotal());
    }

    @Test
    void formatoDeSunat() {
        assertDatos(leer("20555774045|03|B822|432533|79.93|523.97|2025-11-25|1|12345678|Lf1Ugeah9msFncC4Yr+An3JWUTo="),
                "20555774045", "B822", "432533", "2025-11-25", "523.97");
        assertDatos(leer("20504208843|01|FH15|00545390|4.86|31.85|2026-10-25|6|10123456789|UrDiVIrPnP35ZYNc="),
                "20504208843", "FH15", "545390", "2026-10-25", "31.85");
        // Sin la barra final y con el adquirente vacío.
        assertDatos(leer("20613787730|03|B001|0682407|5.71|37.42|2026-10-24|1|12345678"),
                "20613787730", "B001", "682407", "2026-10-24", "37.42");
        assertDatos(leer("10756052328|03|BB01|3262|8.10|53.10|2026-10-16|-||"),
                "10756052328", "BB01", "3262", "2026-10-16", "53.10");
    }

    @Test
    void serieYNumeroJuntosConFechaDiaMesAnio() {
        assertDatos(leer("20100190797|03|B952-3956000|28.37|186.00|04/12/2025|1|LE12345678|"),
                "20100190797", "B952", "3956000", "2025-12-04", "186.00");
    }

    @Test
    void serieSinLetraTomaLaDelTipo() {
        assertDatos(leer("20545695210|03|020-02517975|2.61|17.07|2025-11-26|1|12345678|"),
                "20545695210", "B020", "2517975", "2025-11-26", "17.07");
        assertEquals("F020", leer("20545695210|01|020-02517975|2.61|17.07|2025-11-26|6|10123456789|").serie());
        assertNull(leer("20545695210|07|020-02517975|2.61|17.07|2025-11-26|1|12345678|").serie());
    }

    @Test
    void sinRucSerieNiNumeroSoloFechaYTotal() {
        DatosQr datos = leer("|7.32|47,95|2025-12-01|1|12345678|");
        assertNull(datos.ruc());
        assertNull(datos.serie());
        assertNull(datos.numero());
        assertEquals(LocalDate.of(2025, 12, 1), datos.fechaEmision());
        assertEquals(new BigDecimal("47.95"), datos.importeTotal());
    }

    @Test
    void lineasClaveValor() {
        String texto = "NumFac:B101-59541\nFecFac:22/12/2025\nHorFac:17:11:07-05:00\nNitFac:20610787755\n"
                + "DocAdq:12345678\nValFac:56.50\nValIva:10.17\nValOtroIm:0.00\nValTolFac:66.68\nCUFE:\nQRCode:";
        assertDatos(leer(texto), "20610787755", "B101", "59541", "2025-12-22", "66.68");
    }

    @Test
    void rucInvalidoSeDescarta() {
        DatosQr datos = leer("20555774046|03|B822|432533|79.93|523.97|2025-11-25|1|12345678|");
        assertNull(datos.ruc());
        assertNull(datos.serie());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "https://drive.google.com/file/d/abc/view?usp=sharing",
        "000201010211263700028001039030220250411001119727320085204482953036045802PE5912LCN PERU SAC6004Lima",
        "20555774045|03|B822|432533|79.93|523.97|sin fecha",
        "20555774045|03|B822|432533|79.93|gratis|2025-11-25|1|",
        "NumFac:B101-59541\nValTolFac:66.68",
    })
    void loQueNoEsQrDeComprobante(String texto) {
        assertFalse(QrSunat.interpretar(texto).isPresent());
    }

    @Test
    void primeroSaltaLosQrQueNoSonDelComprobante() {
        assertEquals("FH15", QrSunat.primero(List.of("https://drive.google.com/file/d/abc/view",
                "20504208843|01|FH15|00524695|7.86|51.55|2026-10-04|6|10123456789|")).orElseThrow().serie());
        assertFalse(QrSunat.primero(List.of("https://drive.google.com/file/d/abc/view")).isPresent());
    }

    private static ResultadoExtraccion delModelo(String ruc, String serie, String numero, String fecha,
                                                 String total) {
        String[] valores = {ruc, "LECHE GLORIA S.A.", serie, numero, fecha, "PEN", total};
        List<CampoExtraido> campos = new ArrayList<>();
        for (CampoFactura campo : CampoFactura.values()) {
            String valor = valores[campo.ordinal()];
            campos.add(CampoExtraido.leido(campo, valor, valor == null ? 0 : 0.5));
        }
        return new ResultadoExtraccion("lora_v2_768", 32_000, campos);
    }

    private static CampoExtraido campo(ResultadoExtraccion resultado, CampoFactura nombre) {
        return resultado.campo(nombre).orElseThrow();
    }

    @Test
    void elQrCorrigeLoQueLeyoMalElModelo() {
        DatosQr qr = leer("20100190797|03|B952-4838245|19.85|130.13|12/10/2026|1|LE12345678|");
        ResultadoExtraccion resultado = QrSunat.completar(
                delModelo("20100190787", "B925", "4838246", "2026-10-21", "180.13"), qr);

        assertEquals("20100190797", campo(resultado, CampoFactura.RUC).valorLeido());
        assertEquals("B952", campo(resultado, CampoFactura.SERIE).valorLeido());
        assertEquals("4838245", campo(resultado, CampoFactura.NUMERO).valorLeido());
        assertEquals("2026-10-12", campo(resultado, CampoFactura.FECHA_EMISION).valorLeido());
        assertEquals("130.13", campo(resultado, CampoFactura.IMPORTE_TOTAL).valorLeido());
        assertEquals(1.0, campo(resultado, CampoFactura.RUC).confianza());
        // La razón social y la moneda no van en el QR: quedan las del modelo.
        assertEquals(0.5, campo(resultado, CampoFactura.RAZON_SOCIAL).confianza());
        assertEquals("PEN", campo(resultado, CampoFactura.MONEDA).valorLeido());
        assertEquals("lora_v2_768", resultado.versionModelo());
        assertEquals(List.of(CampoFactura.RAZON_SOCIAL, CampoFactura.MONEDA), resultado.camposARevisar());
    }

    @Test
    void conservaLosCerosImpresosYElTotalConPercepcion() {
        DatosQr qr = leer("20504208843|01|FH15|00545390|4.86|31.85|2026-10-25|6|10123456789|");
        ResultadoExtraccion resultado = QrSunat.completar(
                delModelo("20504208843", "FH15", "00545390", "2026-10-25", "32.49"), qr);

        assertEquals("00545390", campo(resultado, CampoFactura.NUMERO).valorLeido());
        assertEquals("32.49", campo(resultado, CampoFactura.IMPORTE_TOTAL).valorLeido());
        assertEquals(1.0, campo(resultado, CampoFactura.IMPORTE_TOTAL).confianza());
    }

    @Test
    void unTotalMuyPorEncimaDelQrNoEsPercepcion() {
        DatosQr qr = leer("20504208843|01|FH15|00545390|4.86|31.85|2026-10-25|6|10123456789|");
        assertEquals("31.85", campo(QrSunat.completar(
                delModelo(null, null, null, null, "32.50"), qr), CampoFactura.IMPORTE_TOTAL).valorLeido());
        assertEquals("31.85", campo(QrSunat.completar(
                delModelo(null, null, null, null, "31.80"), qr), CampoFactura.IMPORTE_TOTAL).valorLeido());
    }

    @Test
    void loQueFaltaEnElQrQuedaComoLoLeyoElModelo() {
        DatosQr qr = leer("|7.32|47,95|2025-12-01|1|12345678|");
        ResultadoExtraccion resultado = QrSunat.completar(
                delModelo("20501345122", "B101", "02364790", "2025-12-10", null), qr);

        assertEquals("20501345122", campo(resultado, CampoFactura.RUC).valorLeido());
        assertEquals(0.5, campo(resultado, CampoFactura.RUC).confianza());
        assertEquals("02364790", campo(resultado, CampoFactura.NUMERO).valorLeido());
        assertEquals("2025-12-01", campo(resultado, CampoFactura.FECHA_EMISION).valorLeido());
        assertEquals("47.95", campo(resultado, CampoFactura.IMPORTE_TOTAL).valorLeido());
    }
}
