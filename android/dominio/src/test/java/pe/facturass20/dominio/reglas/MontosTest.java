package pe.facturass20.dominio.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class MontosTest {

    @Test
    void deDejaEscalaDos() {
        assertEquals(new BigDecimal("1450.00"), Montos.de("1450"));
        assertEquals(new BigDecimal("0.01"), Montos.de("0.005"));
        assertEquals(2, Montos.CERO.scale());
    }

    @Test
    void convierteACentimosYDeVuelta() {
        assertEquals(412050L, Montos.aCentimos(new BigDecimal("4120.5")));
        assertEquals(new BigDecimal("4120.50"), Montos.desdeCentimos(412050L));
    }

    @Test
    void mayorYPositivo() {
        assertEquals(0, Montos.mayor(Montos.de("3000"), Montos.de("3500")).compareTo(Montos.de("3500")));
        assertTrue(Montos.esPositivo(Montos.de("0.01")));
        assertFalse(Montos.esPositivo(Montos.CERO));
        assertFalse(Montos.esPositivo(null));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "1 450,00|1450.00",
        "1,450.00|1450.00",
        "1.450,00|1450.00",
        "S/ 1450|1450.00",
        "1450|1450.00",
        "1450,5|1450.50",
        "1450.5|1450.50",
        "1,450|1450.00",
        "12.450.000|12450000.00",
        ",50|0.50",
        "4 120,50|4120.50",
    })
    void interpretaFormatosComunes(String texto, String esperado) {
        assertEquals(new BigDecimal(esperado), Montos.interpretar(texto).orElseThrow());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "abc", "-5", "12,3456", "S/"})
    void rechazaTextosQueNoSonMontos(String texto) {
        assertTrue(Montos.interpretar(texto).isEmpty());
    }
}
