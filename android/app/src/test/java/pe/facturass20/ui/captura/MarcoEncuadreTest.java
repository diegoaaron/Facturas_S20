package pe.facturass20.ui.captura;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MarcoEncuadreTest {

    /** Franja arriba a la izquierda de la imagen derecha: fácil de seguir al girarla. */
    private static final MarcoEncuadre MARCO = new MarcoEncuadre(0.1, 0.2, 0.5, 0.4);

    private static void assertMarco(MarcoEncuadre esperado, MarcoEncuadre real) {
        assertEquals(esperado.izquierda(), real.izquierda(), 1e-9);
        assertEquals(esperado.arriba(), real.arriba(), 1e-9);
        assertEquals(esperado.derecha(), real.derecha(), 1e-9);
        assertEquals(esperado.abajo(), real.abajo(), 1e-9);
    }

    @Test
    public void sinGiroNoCambia() {
        assertMarco(MARCO, MARCO.enSensor(0));
    }

    @Test
    public void giro90() {
        // La imagen del sensor se gira 90° a la derecha para verla: lo de arriba en pantalla estaba a la izquierda.
        assertMarco(new MarcoEncuadre(0.2, 0.5, 0.4, 0.9), MARCO.enSensor(90));
    }

    @Test
    public void giro180() {
        assertMarco(new MarcoEncuadre(0.5, 0.6, 0.9, 0.8), MARCO.enSensor(180));
    }

    @Test
    public void giro270() {
        assertMarco(new MarcoEncuadre(0.6, 0.1, 0.8, 0.5), MARCO.enSensor(270));
    }

    @Test
    public void girarYVolverDaLoMismo() {
        // 90 y 270 son inversos: volver a llevar al sensor el marco girado dos veces equivale a 180.
        assertMarco(MARCO.enSensor(180), MARCO.enSensor(90).enSensor(90));
        assertMarco(MARCO, MARCO.enSensor(90).enSensor(270));
        assertMarco(MARCO, MARCO.enSensor(-90).enSensor(90));
    }

    @Test
    public void pixelesDentroDelRecorteVisible() {
        assertArrayEquals(new int[]{110, 70, 150, 90}, MARCO.enPixeles(100, 50, 100, 100));
    }

    @Test
    public void completoCubreTodo() {
        assertArrayEquals(new int[]{0, 0, 1280, 960}, MarcoEncuadre.completo().enPixeles(0, 0, 1280, 960));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaMarcoFueraDeLaImagen() {
        new MarcoEncuadre(0.5, 0, 1.2, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaGirosQueNoSonDe90() {
        MARCO.enSensor(45);
    }
}
