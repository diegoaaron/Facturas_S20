package pe.facturass20.ui.captura;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PreparadorFotoTest {

    @Test
    public void submuestreoDejaElLadoMayorEnAlMenos896() {
        assertEquals(1, PreparadorFoto.submuestreo(896));
        assertEquals(1, PreparadorFoto.submuestreo(1791));
        assertEquals(2, PreparadorFoto.submuestreo(1792));
        assertEquals(2, PreparadorFoto.submuestreo(1920));
        assertEquals(4, PreparadorFoto.submuestreo(4032));
        assertEquals(1, PreparadorFoto.submuestreo(500));
    }
}
