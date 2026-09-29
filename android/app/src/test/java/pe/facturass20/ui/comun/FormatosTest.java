package pe.facturass20.ui.comun;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.Test;

public class FormatosTest {

    /** Cambia el espacio de no separación por uno normal para que los esperados se lean fácil. */
    private static String legible(String texto) {
        return texto.replace(Formatos.ESPACIO, ' ');
    }

    @Test
    public void monedaConMilesYDecimales() {
        assertEquals("S/ 4 120,50", legible(Formatos.moneda(new BigDecimal("4120.5"))));
        assertEquals("S/ 386,40", legible(Formatos.moneda(new BigDecimal("386.40"))));
        assertEquals("S/ 1 234 567,00", legible(Formatos.moneda(new BigDecimal("1234567"))));
        assertEquals("S/ 0,00", legible(Formatos.moneda(BigDecimal.ZERO)));
    }

    @Test
    public void monedaRedondeaAMedioCentimo() {
        assertEquals("S/ 10,01", legible(Formatos.moneda(new BigDecimal("10.005"))));
        assertEquals("-S/ 5,00", legible(Formatos.moneda(new BigDecimal("-5"))));
    }

    @Test
    public void monedaCortaQuitaDecimalesSoloSiEsEntero() {
        assertEquals("S/ 5 000", legible(Formatos.monedaCorta(new BigDecimal("5000.00"))));
        assertEquals("S/ 20", legible(Formatos.monedaCorta(new BigDecimal("20"))));
        assertEquals("S/ 879,50", legible(Formatos.monedaCorta(new BigDecimal("879.5"))));
    }

    @Test
    public void fechas() {
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        assertEquals("15/10/2026", Formatos.fecha(fecha));
        assertEquals("15/10", Formatos.diaMes(fecha));
        assertEquals("jueves 15 de octubre", Formatos.fechaLarga(fecha));
        assertEquals("Septiembre 2026", Formatos.periodo(YearMonth.of(2026, 9)));
    }
}
