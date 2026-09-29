package pe.facturass20.ui.acceso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class ControlIntentosTest {

    /** Almacén en memoria con un reloj que se mueve a mano. */
    private static final class AlmacenFalso implements ControlIntentos.Almacen {
        int fallos;
        long hasta;

        @Override
        public int fallos() {
            return fallos;
        }

        @Override
        public long bloqueadoHasta() {
            return hasta;
        }

        @Override
        public void guardar(int fallos, long bloqueadoHasta) {
            this.fallos = fallos;
            this.hasta = bloqueadoHasta;
        }
    }

    private AlmacenFalso almacen;
    private long ahora;
    private ControlIntentos control;

    @Before
    public void preparar() {
        almacen = new AlmacenFalso();
        ahora = 1_000_000L;
        control = new ControlIntentos(almacen, () -> ahora);
    }

    @Test
    public void dosFallosNoBloquean() {
        control.registrarFallo();
        control.registrarFallo();
        assertFalse(control.bloqueado());
        assertEquals(1, control.intentosRestantes());
    }

    @Test
    public void tercerFalloBloquea30Segundos() {
        for (int i = 0; i < 3; i++) {
            control.registrarFallo();
        }
        assertTrue(control.bloqueado());
        assertEquals(30_000L, control.msRestantes());

        ahora += 29_999L;
        assertTrue(control.bloqueado());
        ahora += 1L;
        assertFalse(control.bloqueado());
        assertEquals("tras la espera vuelven los 3 intentos", 3, control.intentosRestantes());
    }

    @Test
    public void aciertoReiniciaLosFallos() {
        control.registrarFallo();
        control.registrarFallo();
        control.reiniciar();
        assertEquals(3, control.intentosRestantes());
        control.registrarFallo();
        assertFalse(control.bloqueado());
    }

    @Test
    public void atrasarLaHoraNoAlargaLaEspera() {
        for (int i = 0; i < 3; i++) {
            control.registrarFallo();
        }
        ahora -= 3_600_000L;
        assertEquals(ControlIntentos.ESPERA_MS, control.msRestantes());
    }

    @Test
    public void laEsperaSobreviveAUnControlNuevo() {
        for (int i = 0; i < 3; i++) {
            control.registrarFallo();
        }
        ControlIntentos otro = new ControlIntentos(almacen, () -> ahora + 10_000L);
        assertEquals(20_000L, otro.msRestantes());
    }
}
