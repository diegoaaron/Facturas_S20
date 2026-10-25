package pe.facturass20.ui.mes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.junit.Test;

import pe.facturass20.ui.comun.Formatos;

/** El teclado de montos de P13. */
public class EntradaMontoTest {

    private static EntradaMonto escribir(String teclas) {
        EntradaMonto entrada = new EntradaMonto();
        for (char tecla : teclas.toCharArray()) {
            if (tecla == ',') {
                entrada.coma();
            } else if (tecla == '<') {
                entrada.borrar();
            } else {
                entrada.digito(tecla);
            }
        }
        return entrada;
    }

    @Test
    public void escribeEnterosYDecimales() {
        EntradaMonto entrada = escribir("3900,5");
        assertEquals("3900,5", entrada.texto());
        assertEquals(new BigDecimal("3900.50"), entrada.monto());
        assertEquals("3" + Formatos.ESPACIO + "900,5", entrada.paraMostrar());
    }

    @Test
    public void admiteSoloDosDecimalesYUnaComa() {
        assertEquals("12,34", escribir("12,345").texto());
        assertEquals("12,3", escribir("12,,3").texto());
    }

    @Test
    public void comaAlInicioPoneCero() {
        EntradaMonto entrada = escribir(",5");
        assertEquals("0,5", entrada.texto());
        assertEquals(new BigDecimal("0.50"), entrada.monto());
    }

    @Test
    public void ceroInicialSeReemplaza() {
        assertEquals("7", escribir("07").texto());
    }

    @Test
    public void limitaLosEnteros() {
        assertEquals("12345678", escribir("123456789").texto());
    }

    @Test
    public void borraYQuedaVacio() {
        EntradaMonto entrada = escribir("12<<<");
        assertTrue(entrada.vacio());
        assertEquals(new BigDecimal("0.00"), entrada.monto());
    }

    @Test
    public void comaFinalSeIgnoraAlConvertir() {
        assertEquals(new BigDecimal("15.00"), escribir("15,").monto());
    }

    @Test
    public void cargaUnMontoGuardado() {
        EntradaMonto entrada = new EntradaMonto();
        entrada.cargar(new BigDecimal("3900.00"));
        assertEquals("3900", entrada.texto());
        entrada.cargar(new BigDecimal("12.50"));
        assertEquals("12,5", entrada.texto());
        entrada.cargar(new BigDecimal("0.00"));
        assertTrue(entrada.vacio());
    }
}
