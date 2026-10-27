package pe.facturass20.ui.captura;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RecortadorComprobanteTest {

    private static final int ANCHO = 800;
    private static final int ALTO = 600;

    /** Mesa gris clara con una hoja blanca de (200, 100) a (600, 500) y renglones de texto dentro. */
    private static byte[] hojaSobreMesa() {
        byte[] luma = new byte[ANCHO * ALTO];
        for (int y = 0; y < ALTO; y++) {
            for (int x = 0; x < ANCHO; x++) {
                boolean papel = x >= 200 && x < 600 && y >= 100 && y < 500;
                boolean texto = x >= 230 && x < 570 && y >= 130 && y < 470 && (y - 130) % 20 < 4 && (x / 6) % 5 != 0;
                luma[y * ANCHO + x] = (byte) (texto ? 30 : papel ? 230 : 190);
            }
        }
        return luma;
    }

    @Test
    public void encuentraLaHojaConMargen() {
        double[] e = RecortadorComprobante.esquinas(hojaSobreMesa(), ANCHO, ALTO);
        assertNotNull(e);
        double izquierda = e[0] * (ANCHO - 1);
        double arriba = e[1] * (ALTO - 1);
        double derecha = e[4] * (ANCHO - 1);
        double abajo = e[5] * (ALTO - 1);
        // El borde de la hoja también cuenta como tinta (la mesa queda más oscura que su entorno), así que
        // el recorte es la hoja, de (200, 100) a (600, 500), más el margen del 3 % por lado.
        assertTrue("izquierda " + izquierda, izquierda < 200 && izquierda > 170);
        assertTrue("arriba " + arriba, arriba < 100 && arriba > 70);
        assertTrue("derecha " + derecha, derecha > 599 && derecha < 630);
        assertTrue("abajo " + abajo, abajo > 499 && abajo < 530);
        // Sin giro: arriba-derecha a la misma altura que arriba-izquierda.
        assertEquals(e[1] * (ALTO - 1), e[3] * (ALTO - 1), 2);
    }

    @Test
    public void sinTintaNoRecorta() {
        byte[] luma = new byte[ANCHO * ALTO];
        java.util.Arrays.fill(luma, (byte) 200);
        assertNull(RecortadorComprobante.esquinas(luma, ANCHO, ALTO));
    }

    @Test
    public void unaManchaChicaNoEsUnComprobante() {
        byte[] luma = new byte[ANCHO * ALTO];
        java.util.Arrays.fill(luma, (byte) 200);
        for (int y = 290; y < 310; y++) {
            for (int x = 380; x < 420; x++) {
                luma[y * ANCHO + x] = 20;
            }
        }
        assertNull(RecortadorComprobante.esquinas(luma, ANCHO, ALTO));
    }

    @Test
    public void ordenaLasEsquinasDeUnRectanguloGirado() {
        double[][] ordenadas = RecortadorComprobante.ordenar(new double[][]{{110, 210}, {10, 20}, {200, 15}, {20, 190}});
        assertEquals(10, ordenadas[0][0], 0);   // arriba-izquierda
        assertEquals(200, ordenadas[1][0], 0);  // arriba-derecha
        assertEquals(110, ordenadas[2][0], 0);  // abajo-derecha
        assertEquals(20, ordenadas[3][0], 0);   // abajo-izquierda
    }
}
