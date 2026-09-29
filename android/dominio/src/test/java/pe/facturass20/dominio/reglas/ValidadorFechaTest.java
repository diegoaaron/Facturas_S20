package pe.facturass20.dominio.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import pe.facturass20.dominio.modelo.EstadoPeriodo;
import pe.facturass20.dominio.reglas.ValidadorFecha.Resultado;

class ValidadorFechaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 5);

    @Test
    void interpretaIsoYDiaMesAnio() {
        assertEquals(LocalDate.of(2026, 9, 22), ValidadorFecha.interpretar("2026-09-22").orElseThrow());
        assertEquals(LocalDate.of(2026, 9, 22), ValidadorFecha.interpretar(" 22/09/2026 ").orElseThrow());
        assertEquals(LocalDate.of(2026, 9, 2), ValidadorFecha.interpretar("2/9/2026").orElseThrow());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  ", "31/02/2026", "2026-13-01", "22-09-2026", "ayer"})
    void rechazaFechasInvalidas(String texto) {
        assertTrue(ValidadorFecha.interpretar(texto).isEmpty());
    }

    @Test
    void evaluaSegunElMes() {
        assertEquals(Resultado.DEL_MES, ValidadorFecha.evaluar(LocalDate.of(2026, 10, 1), HOY, null));
        assertEquals(Resultado.DEL_MES, ValidadorFecha.evaluar(HOY, HOY, EstadoPeriodo.ABIERTO));
        assertEquals(Resultado.DEL_MES_ANTERIOR,
                ValidadorFecha.evaluar(LocalDate.of(2026, 9, 30), HOY, EstadoPeriodo.ABIERTO));
        assertEquals(Resultado.PERIODO_CERRADO,
                ValidadorFecha.evaluar(LocalDate.of(2026, 9, 30), HOY, EstadoPeriodo.CERRADO));
        assertEquals(Resultado.REQUIERE_CONFIRMACION, ValidadorFecha.evaluar(LocalDate.of(2026, 8, 31), HOY, null));
        assertEquals(Resultado.FUTURA, ValidadorFecha.evaluar(HOY.plusDays(1), HOY, null));
    }

    @Test
    void cambioDeAnio() {
        assertEquals(Resultado.DEL_MES_ANTERIOR,
                ValidadorFecha.evaluar(LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 3), null));
    }

    @Test
    void permiteRegistrar() {
        assertTrue(Resultado.DEL_MES.permiteRegistrar());
        assertTrue(Resultado.REQUIERE_CONFIRMACION.permiteRegistrar());
        assertFalse(Resultado.PERIODO_CERRADO.permiteRegistrar());
        assertFalse(Resultado.FUTURA.permiteRegistrar());
    }
}
