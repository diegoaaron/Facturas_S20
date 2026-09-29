package pe.facturass20.ui.acceso;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CreacionPinTest {

    @Test
    public void pideRepetirYConfirma() {
        CreacionPin creacion = new CreacionPin();
        assertEquals(CreacionPin.Resultado.REPETIR, creacion.ingresar("1234".toCharArray()));
        assertTrue(creacion.esperandoRepeticion());
        assertEquals(CreacionPin.Resultado.LISTO, creacion.ingresar("1234".toCharArray()));
        assertArrayEquals("1234".toCharArray(), creacion.pin());
        assertFalse(creacion.esperandoRepeticion());
    }

    @Test
    public void siNoCoincideEmpiezaDeNuevo() {
        CreacionPin creacion = new CreacionPin();
        creacion.ingresar("1234".toCharArray());
        assertEquals(CreacionPin.Resultado.NO_COINCIDE, creacion.ingresar("4321".toCharArray()));
        assertNull(creacion.pin());
        assertEquals(CreacionPin.Resultado.REPETIR, creacion.ingresar("4321".toCharArray()));
        assertEquals(CreacionPin.Resultado.LISTO, creacion.ingresar("4321".toCharArray()));
    }

    @Test
    public void copiaElArregloRecibido() {
        CreacionPin creacion = new CreacionPin();
        char[] escrito = "5678".toCharArray();
        creacion.ingresar(escrito);
        escrito[0] = '0';
        assertEquals(CreacionPin.Resultado.LISTO, creacion.ingresar("5678".toCharArray()));
    }

    @Test
    public void reiniciarBorraElPin() {
        CreacionPin creacion = new CreacionPin();
        creacion.ingresar("1111".toCharArray());
        creacion.ingresar("1111".toCharArray());
        char[] pin = creacion.pin();
        creacion.reiniciar();
        assertNull(creacion.pin());
        assertArrayEquals("el arreglo entregado también se borra", "0000".toCharArray(), pin);
    }
}
