package pe.facturass20.inferencia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Random;

import org.junit.Test;

import pe.facturass20.dominio.casosuso.VerificarFactura;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;
import pe.facturass20.dominio.reglas.ValidadorCampos;

/** El lector de prueba debe devolver siempre una factura que pase las validaciones de P08. */
public class ExtractorFalsoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 24);

    @Test
    public void devuelveLosSieteCamposValidosConLaFechaDeHoy() throws Exception {
        ExtractorFalso extractor = new ExtractorFalso(new Random(7), () -> HOY, 0);
        for (int i = 0; i < 20; i++) {
            ResultadoExtraccion resultado = extractor.extraer(new byte[0]);
            assertEquals(CampoFactura.values().length, resultado.campos().size());
            VerificarFactura.Datos datos = VerificarFactura.Datos.desde(resultado);
            for (CampoFactura campo : CampoFactura.values()) {
                assertTrue(campo + ": " + datos.valor(campo),
                        ValidadorCampos.error(campo, datos.valor(campo)).isEmpty());
            }
            assertEquals("2026-10-24", datos.fechaEmision());
        }
    }

    @Test
    public void elImporteSaleEnAmbarParaRevisarlo() throws Exception {
        ResultadoExtraccion resultado = new ExtractorFalso(new Random(1), () -> HOY, 0).extraer(new byte[0]);
        assertEquals(java.util.Collections.singletonList(CampoFactura.IMPORTE_TOTAL), resultado.camposARevisar());
    }
}
