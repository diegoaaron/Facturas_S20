package pe.facturass20.ui.captura;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EvaluadorNitidezTest {

    private static final int ANCHO = 64;
    private static final int ALTO = 48;

    /** Tablero de cuadros de {@code lado} px entre {@code oscuro} y {@code claro}. */
    private static byte[] tablero(int ancho, int alto, int lado, int oscuro, int claro) {
        byte[] luma = new byte[ancho * alto];
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                luma[y * ancho + x] = (byte) (((x / lado + y / lado) % 2 == 0) ? oscuro : claro);
            }
        }
        return luma;
    }

    private static byte[] uniforme(int valor) {
        byte[] luma = new byte[ANCHO * ALTO];
        java.util.Arrays.fill(luma, (byte) valor);
        return luma;
    }

    /** Degradado horizontal suave: se ve «borroso» aunque tenga contraste. */
    private static byte[] degradado() {
        byte[] luma = new byte[ANCHO * ALTO];
        for (int y = 0; y < ALTO; y++) {
            for (int x = 0; x < ANCHO; x++) {
                luma[y * ANCHO + x] = (byte) (60 + x * 2);
            }
        }
        return luma;
    }

    @Test
    public void imagenUniformeNoTieneNitidez() {
        EvaluadorNitidez.Evaluacion evaluacion = EvaluadorNitidez.evaluar(uniforme(128), ANCHO, ALTO);
        assertEquals(0, evaluacion.nitidez(), 1e-9);
        assertEquals(128, evaluacion.luz(), 1e-9);
        assertFalse(evaluacion.nitidezBuena());
        assertFalse(evaluacion.legible());
    }

    @Test
    public void bordesMarcadosDanNitidezBuena() {
        EvaluadorNitidez.Evaluacion evaluacion = EvaluadorNitidez.evaluar(tablero(ANCHO, ALTO, 4, 40, 220), ANCHO, ALTO);
        assertTrue(evaluacion.nitidez() > EvaluadorNitidez.UMBRAL_NITIDEZ);
        assertEquals(130, evaluacion.luz(), 1);
        assertTrue(evaluacion.legible());
    }

    @Test
    public void degradadoSuaveEsBorroso() {
        assertFalse(EvaluadorNitidez.evaluar(degradado(), ANCHO, ALTO).nitidezBuena());
    }

    @Test
    public void luzSeClasificaPorLaMedia() {
        assertEquals(EvaluadorNitidez.Luz.BAJA, EvaluadorNitidez.evaluar(uniforme(30), ANCHO, ALTO).nivelLuz());
        assertEquals(EvaluadorNitidez.Luz.BUENA, EvaluadorNitidez.evaluar(uniforme(60), ANCHO, ALTO).nivelLuz());
        assertEquals(EvaluadorNitidez.Luz.BUENA, EvaluadorNitidez.evaluar(uniforme(200), ANCHO, ALTO).nivelLuz());
        assertEquals(EvaluadorNitidez.Luz.EXCESIVA, EvaluadorNitidez.evaluar(uniforme(230), ANCHO, ALTO).nivelLuz());
    }

    @Test
    public void nitidaPeroOscuraNoEsLegible() {
        EvaluadorNitidez.Evaluacion evaluacion = EvaluadorNitidez.evaluar(tablero(ANCHO, ALTO, 4, 0, 90), ANCHO, ALTO);
        assertTrue(evaluacion.nitidezBuena());
        assertEquals(EvaluadorNitidez.Luz.BAJA, evaluacion.nivelLuz());
        assertFalse(evaluacion.legible());
    }

    @Test
    public void soloMideDentroDeLaRegion() {
        // Izquierda con bordes, derecha lisa: la región derecha no tiene nitidez.
        byte[] luma = tablero(ANCHO, ALTO, 4, 40, 220);
        for (int y = 0; y < ALTO; y++) {
            for (int x = ANCHO / 2; x < ANCHO; x++) {
                luma[y * ANCHO + x] = (byte) 150;
            }
        }
        EvaluadorNitidez.Evaluacion derecha = EvaluadorNitidez.evaluar(luma, ANCHO, ANCHO / 2 + 1, 0, ANCHO, ALTO);
        assertEquals(0, derecha.nitidez(), 1e-9);
        assertEquals(150, derecha.luz(), 1e-9);
        assertTrue(EvaluadorNitidez.evaluar(luma, ANCHO, 0, 0, ANCHO / 2, ALTO).nitidezBuena());
    }

    @Test
    public void respetaElPasoDeFila() {
        // Cada fila trae 16 bytes de relleno (como el plano Y de la cámara) que no deben contar.
        int paso = ANCHO + 16;
        byte[] luma = new byte[paso * ALTO];
        java.util.Arrays.fill(luma, (byte) 255);
        for (int y = 0; y < ALTO; y++) {
            for (int x = 0; x < ANCHO; x++) {
                luma[y * paso + x] = (byte) 100;
            }
        }
        EvaluadorNitidez.Evaluacion evaluacion = EvaluadorNitidez.evaluar(luma, paso, 0, 0, ANCHO, ALTO);
        assertEquals(0, evaluacion.nitidez(), 1e-9);
        assertEquals(100, evaluacion.luz(), 1e-9);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaRegionesMinimas() {
        EvaluadorNitidez.evaluar(uniforme(100), ANCHO, 0, 0, 2, 2);
    }
}
