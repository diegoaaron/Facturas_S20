package pe.facturass20.ui.verificacion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.CampoFactura;

/** Colores de los campos de P08 y P19 (RF-05). */
public class RevisionCampoTest {

    private static final CampoExtraido IMPORTE_DUDOSO = CampoExtraido.leido(CampoFactura.IMPORTE_TOTAL, "1450.00", 0.62);

    @Test
    public void confianzaBajaSinTocarPideRevisar() {
        RevisionCampo revision = RevisionCampo.revisar(CampoFactura.IMPORTE_TOTAL, "1450.00", "1450.00", IMPORTE_DUDOSO);
        assertEquals(RevisionCampo.Estado.REVISAR, revision.estado());
        assertEquals(62, revision.confianzaPorcentaje());
    }

    @Test
    public void siElUsuarioLoCambiaQuedaValido() {
        RevisionCampo revision = RevisionCampo.revisar(CampoFactura.IMPORTE_TOTAL, "1540.00", "1450.00", IMPORTE_DUDOSO);
        assertEquals(RevisionCampo.Estado.VALIDO, revision.estado());
    }

    @Test
    public void confianzaAltaEsValido() {
        CampoExtraido ruc = CampoExtraido.leido(CampoFactura.RUC, "20601234565", 0.98);
        assertEquals(RevisionCampo.Estado.VALIDO,
                RevisionCampo.revisar(CampoFactura.RUC, "20601234565", "20601234565", ruc).estado());
    }

    @Test
    public void rucConDigitoVerificadorMaloEsInvalido() {
        RevisionCampo revision = RevisionCampo.revisar(CampoFactura.RUC, "20601234566", null, null);
        assertEquals(RevisionCampo.Estado.INVALIDO, revision.estado());
        assertNotNull(revision.mensaje());
    }

    @Test
    public void campoVacioEsInvalido() {
        assertEquals(RevisionCampo.Estado.INVALIDO,
                RevisionCampo.revisar(CampoFactura.RAZON_SOCIAL, "", null, null).estado());
    }

    @Test
    public void muestraFechaEImporteComoLosEscribeElUsuario() {
        assertEquals("22/09/2026", RevisionCampo.paraMostrar(CampoFactura.FECHA_EMISION, "2026-09-22"));
        assertEquals("1450.00", RevisionCampo.paraMostrar(CampoFactura.IMPORTE_TOTAL, "1450.00"));
        assertEquals("F001", RevisionCampo.paraMostrar(CampoFactura.SERIE, " F001 "));
        assertEquals("", RevisionCampo.paraMostrar(CampoFactura.NUMERO, null));
    }
}
