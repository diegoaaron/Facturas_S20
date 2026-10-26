package pe.facturass20.inferencia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.List;

import org.junit.Test;

import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.puertos.ExtraccionException;

/** Reglas de §6.3 para leer lo que responde el modelo. */
public class ParserRespuestaTest {

    private static final String EJEMPLO = "{\n"
            + "  \"ruc\":           { \"valor\": \"20601234565\", \"confianza\": 0.98 },\n"
            + "  \"razon_social\":  { \"valor\": \"DISTRIBUIDORA ANDINA S.A.C.\", \"confianza\": 0.95 },\n"
            + "  \"serie\":         { \"valor\": \"F001\", \"confianza\": 0.99 },\n"
            + "  \"numero\":        { \"valor\": \"004821\", \"confianza\": 0.97 },\n"
            + "  \"fecha_emision\": { \"valor\": \"2026-09-22\", \"confianza\": 0.96 },\n"
            + "  \"moneda\":        { \"valor\": \"PEN\", \"confianza\": 0.99 },\n"
            + "  \"importe_total\": { \"valor\": \"1450.00\", \"confianza\": 0.62 }\n"
            + "}";

    /** Esquema anterior, con valor y confianza por campo: se sigue aceptando. */
    @Test
    public void leeElEsquemaAnteriorConConfianza() throws Exception {
        List<CampoExtraido> campos = ParserRespuesta.interpretar(EJEMPLO);
        assertEquals(7, campos.size());
        assertCampo(campos, CampoFactura.RUC, "20601234565", 0.98);
        assertCampo(campos, CampoFactura.RAZON_SOCIAL, "DISTRIBUIDORA ANDINA S.A.C.", 0.95);
        assertCampo(campos, CampoFactura.SERIE, "F001", 0.99);
        assertCampo(campos, CampoFactura.NUMERO, "004821", 0.97);
        assertCampo(campos, CampoFactura.FECHA_EMISION, "2026-09-22", 0.96);
        assertCampo(campos, CampoFactura.MONEDA, "PEN", 0.99);
        assertCampo(campos, CampoFactura.IMPORTE_TOTAL, "1450.00", 0.62);
    }

    @Test
    public void encuentraElJsonDentroDeTextoYDeUnBloqueDeCodigo() throws Exception {
        String respuesta = "Claro, estos son los datos {de la factura}:\n```json\n" + EJEMPLO + "\n```\nListo.";
        assertCampo(ParserRespuesta.interpretar(respuesta), CampoFactura.RUC, "20601234565", 0.98);
    }

    @Test
    public void lasLlavesDentroDeLosTextosNoConfundenElFinDelObjeto() throws Exception {
        String respuesta = "{\"razon_social\": {\"valor\": \"ABARROTES {EL} PUENTE\", \"confianza\": 0.9}}";
        assertCampo(ParserRespuesta.interpretar(respuesta), CampoFactura.RAZON_SOCIAL, "ABARROTES {EL} PUENTE", 0.9);
    }

    @Test
    public void normalizaLosValores() throws Exception {
        String respuesta = "{"
                + "\"ruc\": {\"valor\": \"RUC 20601234565\", \"confianza\": 0.9},"
                + "\"razon_social\": {\"valor\": \"  COMERCIAL   LIMA  \", \"confianza\": 0.9},"
                + "\"serie\": {\"valor\": \"f 001\", \"confianza\": 0.9},"
                + "\"numero\": {\"valor\": \" 004821 \", \"confianza\": 0.9},"
                + "\"fecha_emision\": {\"valor\": \"22/09/2026\", \"confianza\": 0.9},"
                + "\"moneda\": {\"valor\": \"Soles\", \"confianza\": 0.9},"
                + "\"importe_total\": {\"valor\": \"S/ 1 450,00\", \"confianza\": 0.9}}";
        List<CampoExtraido> campos = ParserRespuesta.interpretar(respuesta);
        assertCampo(campos, CampoFactura.RUC, "20601234565", 0.9);
        assertCampo(campos, CampoFactura.RAZON_SOCIAL, "COMERCIAL LIMA", 0.9);
        assertCampo(campos, CampoFactura.SERIE, "F001", 0.9);
        assertCampo(campos, CampoFactura.NUMERO, "004821", 0.9);
        assertCampo(campos, CampoFactura.FECHA_EMISION, "2026-09-22", 0.9);
        assertCampo(campos, CampoFactura.MONEDA, "PEN", 0.9);
        assertCampo(campos, CampoFactura.IMPORTE_TOTAL, "1450.00", 0.9);
    }

    @Test
    public void aceptaImportesYFechasEnOtrosFormatos() {
        assertEquals("1450.00", ParserRespuesta.normalizar(CampoFactura.IMPORTE_TOTAL, "1,450.00"));
        assertEquals("1450.00", ParserRespuesta.normalizar(CampoFactura.IMPORTE_TOTAL, "1450"));
        assertEquals("945.20", ParserRespuesta.normalizar(CampoFactura.IMPORTE_TOTAL, "945.2"));
        assertEquals("2026-09-22", ParserRespuesta.normalizar(CampoFactura.FECHA_EMISION, "22-09-2026"));
        assertEquals("2026-09-02", ParserRespuesta.normalizar(CampoFactura.FECHA_EMISION, "2.9.2026"));
        assertEquals("USD", ParserRespuesta.normalizar(CampoFactura.MONEDA, "US$"));
        assertNull(ParserRespuesta.normalizar(CampoFactura.FECHA_EMISION, "31/02/2026"));
        assertNull(ParserRespuesta.normalizar(CampoFactura.IMPORTE_TOTAL, "-20.00"));
        assertNull(ParserRespuesta.normalizar(CampoFactura.MONEDA, "EUR"));
    }

    @Test
    public void unCampoQueFaltaOEsNuloQuedaSinValorYConConfianzaCero() throws Exception {
        String respuesta = "{\"ruc\": {\"valor\": null, \"confianza\": 0.7},"
                + "\"fecha_emision\": {\"valor\": \"ayer\", \"confianza\": 0.9}}";
        List<CampoExtraido> campos = ParserRespuesta.interpretar(respuesta);
        assertCampo(campos, CampoFactura.RUC, null, 0);
        assertCampo(campos, CampoFactura.FECHA_EMISION, null, 0);
        assertCampo(campos, CampoFactura.SERIE, null, 0);
    }

    @Test
    public void leeElFormatoPlanoDelModeloAfinado() throws Exception {
        String respuesta = "{\"ruc\":\"20611981202\",\"razon_social\":\"DINNO SUR E.I.R.L.\",\"serie\":\"B001\","
                + "\"numero\":\"00308984\",\"fecha_emision\":\"2025-12-20\",\"moneda\":\"PEN\","
                + "\"importe_total\":\"131.31\"}";
        List<CampoExtraido> campos = ParserRespuesta.interpretar(respuesta);
        double validado = ParserRespuesta.CONFIANZA_VALIDADO;
        assertCampo(campos, CampoFactura.RUC, "20611981202", validado);
        assertCampo(campos, CampoFactura.RAZON_SOCIAL, "DINNO SUR E.I.R.L.", validado);
        assertCampo(campos, CampoFactura.SERIE, "B001", validado);
        assertCampo(campos, CampoFactura.NUMERO, "00308984", validado);
        assertCampo(campos, CampoFactura.FECHA_EMISION, "2025-12-20", validado);
        assertCampo(campos, CampoFactura.MONEDA, "PEN", validado);
        assertCampo(campos, CampoFactura.IMPORTE_TOTAL, "131.31", validado);
    }

    @Test
    public void sinConfianzaLaDeduceDeLaValidacion() throws Exception {
        String respuesta = "{\"ruc\": 20601234565, \"importe_total\": 1450.5, \"moneda\": {\"valor\": \"PEN\"},"
                + "\"serie\": \"F0001\", \"razon_social\": null}";
        List<CampoExtraido> campos = ParserRespuesta.interpretar(respuesta);
        assertCampo(campos, CampoFactura.RUC, "20601234565", ParserRespuesta.CONFIANZA_VALIDADO);
        assertCampo(campos, CampoFactura.IMPORTE_TOTAL, "1450.50", ParserRespuesta.CONFIANZA_VALIDADO);
        assertCampo(campos, CampoFactura.MONEDA, "PEN", ParserRespuesta.CONFIANZA_VALIDADO);
        assertCampo(campos, CampoFactura.SERIE, "F0001", ParserRespuesta.CONFIANZA_SIN_VALIDAR);
        assertCampo(campos, CampoFactura.RAZON_SOCIAL, null, 0);
    }

    @Test
    public void unRucConDigitoVerificadorErradoQuedaParaRevisar() throws Exception {
        List<CampoExtraido> campos = ParserRespuesta.interpretar("{\"ruc\":\"20601234566\",\"moneda\":\"USD\"}");
        assertCampo(campos, CampoFactura.RUC, "20601234566", ParserRespuesta.CONFIANZA_SIN_VALIDAR);
        // USD se lee bien, pero por ahora solo se registran compras en soles.
        assertCampo(campos, CampoFactura.MONEDA, "USD", ParserRespuesta.CONFIANZA_SIN_VALIDAR);
    }

    @Test
    public void enElFormatoPlanoSeparaLaSerieYValidaCadaParte() throws Exception {
        List<CampoExtraido> campos = ParserRespuesta.interpretar("{\"numero\":\"F001-004521\"}");
        assertCampo(campos, CampoFactura.SERIE, "F001", ParserRespuesta.CONFIANZA_VALIDADO);
        assertCampo(campos, CampoFactura.NUMERO, "004521", ParserRespuesta.CONFIANZA_VALIDADO);
    }

    @Test
    public void laConfianzaSeLimitaEntreCeroYUno() throws Exception {
        String respuesta = "{\"ruc\": {\"valor\": \"20601234565\", \"confianza\": 98},"
                + "\"serie\": {\"valor\": \"F001\", \"confianza\": \"0,7\"}}";
        List<CampoExtraido> campos = ParserRespuesta.interpretar(respuesta);
        assertCampo(campos, CampoFactura.RUC, "20601234565", 1);
        assertCampo(campos, CampoFactura.SERIE, "F001", 0.7);
    }

    @Test
    public void separaLaSerieCuandoVieneEnElNumero() throws Exception {
        String respuesta = "{\"numero\": {\"valor\": \"F001-004521\", \"confianza\": 0.9}}";
        List<CampoExtraido> campos = ParserRespuesta.interpretar(respuesta);
        assertCampo(campos, CampoFactura.SERIE, "F001", 0.9);
        assertCampo(campos, CampoFactura.NUMERO, "004521", 0.9);
    }

    @Test
    public void sinJsonPideOtraFoto() {
        ExtraccionException error = assertThrows(ExtraccionException.class,
                () -> ParserRespuesta.interpretar("No puedo leer esta imagen."));
        assertEquals(ParserRespuesta.SIN_JSON, error.getMessage());
        assertThrows(ExtraccionException.class, () -> ParserRespuesta.interpretar("{\"ruc\": "));
        assertThrows(ExtraccionException.class, () -> ParserRespuesta.interpretar(null));
    }

    private static void assertCampo(List<CampoExtraido> campos, CampoFactura nombre, String valor, double confianza) {
        CampoExtraido campo = campos.get(nombre.ordinal());
        assertEquals(nombre, campo.nombre());
        assertEquals(nombre.name(), valor, campo.valorLeido());
        assertEquals(nombre.name(), confianza, campo.confianza(), 1e-9);
    }
}
