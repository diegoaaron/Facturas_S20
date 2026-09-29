package pe.facturass20.dominio.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.facturass20.dominio.Datos;
import pe.facturass20.dominio.modelo.ConfiguracionNrusException;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.EstadoPeriodo;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.NivelAlerta;
import pe.facturass20.dominio.modelo.ParametrosNrus;
import pe.facturass20.dominio.modelo.PeriodoMensual;

/** Los 7 casos mínimos de la documentación técnica §5.4, más tope anual y errores de configuración. */
class MotorReglasNRUSTest {

    private static final int DIGITO = 4;
    private final MotorReglasNRUS motor = new MotorReglasNRUS(Datos.parametros());

    private static PeriodoMensual mes(String ventas, String... compras) {
        List<FacturaCompra> facturas = new ArrayList<>();
        for (int i = 0; i < compras.length; i++) {
            facturas.add(Datos.guardada(i + 1, String.valueOf(i + 1), compras[i]));
        }
        return new PeriodoMensual(Datos.SEPTIEMBRE, ventas == null ? null : Montos.de(ventas),
                EstadoPeriodo.ABIERTO, facturas);
    }

    private static void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, Montos.de(esperado).compareTo(real), () -> "esperado " + esperado + ", fue " + real);
    }

    @Test
    void caso1ComprasDeterminanYAvisa80() {
        Determinacion d = motor.determinar(mes("3900.00", "4120.50"), DIGITO);
        assertEquals(1, d.categoria().codigo());
        assertMonto("20.00", d.cuota());
        assertMonto("4120.50", d.montoDeterminante());
        assertEquals(NivelAlerta.AVISO_80, d.nivelAlerta());
    }

    @Test
    void caso2VentasDeterminan() {
        Determinacion d = motor.determinar(mes("3500.00", "3000.00"), DIGITO);
        assertEquals(1, d.categoria().codigo());
        assertMonto("20.00", d.cuota());
        assertMonto("3500.00", d.montoDeterminante());
        assertMonto("3000.00", d.totalAdquisiciones());
        assertEquals(NivelAlerta.NINGUNA, d.nivelAlerta());
    }

    @Test
    void caso3LimiteInclusivo() {
        Determinacion d = motor.determinar(mes("0", "5000.00"), DIGITO);
        assertEquals(1, d.categoria().codigo());
        assertEquals(NivelAlerta.AVISO_80, d.nivelAlerta());
    }

    @Test
    void caso4PasaACategoria2() {
        Determinacion d = motor.determinar(mes("0", "5000.01"), DIGITO);
        assertEquals(2, d.categoria().codigo());
        assertMonto("50.00", d.cuota());
        assertEquals(NivelAlerta.LIMITE_100, d.nivelAlerta());
    }

    @Test
    void caso5FueraDeRegimen() {
        Determinacion d = motor.determinar(mes("0", "8000.01"), DIGITO);
        assertTrue(d.fueraDeRegimen());
        assertNull(d.categoria());
        assertNull(d.cuota());
        assertEquals(NivelAlerta.FUERA_DE_REGIMEN, d.nivelAlerta());
    }

    @Test
    void caso6MesVacio() {
        Determinacion d = motor.determinar(mes("0"), DIGITO);
        assertEquals(1, d.categoria().codigo());
        assertMonto("20.00", d.cuota());
        assertMonto("0", d.montoDeterminante());
        assertEquals(NivelAlerta.NINGUNA, d.nivelAlerta());
    }

    @Test
    void caso7FacturaAnuladaNoSuma() {
        PeriodoMensual periodo = mes("0", "3734.10", "386.40");
        periodo.anularFactura(2, "Error de registro");
        Determinacion d = motor.determinar(periodo, DIGITO);
        assertMonto("3734.10", d.totalAdquisiciones());
        assertEquals(NivelAlerta.NINGUNA, d.nivelAlerta());
    }

    @Test
    void ventasSinRegistrarCuentanComoCero() {
        Determinacion d = motor.determinar(mes(null, "1000.00"), DIGITO);
        assertMonto("1000.00", d.montoDeterminante());
    }

    @Test
    void limiteExactoDeLaUltimaCategoriaEsLimite100() {
        Determinacion d = motor.determinar(mes("8000.00"), DIGITO);
        assertEquals(2, d.categoria().codigo());
        assertEquals(NivelAlerta.LIMITE_100, d.nivelAlerta());
    }

    @Test
    void vencimientoSegunCronograma() {
        Determinacion d = motor.determinar(mes("0"), DIGITO);
        assertEquals(LocalDate.of(2026, 10, 15), d.fechaVencimiento());
        assertEquals(Datos.SEPTIEMBRE, d.periodo());
    }

    @Test
    void sinFechaEnElCronogramaEsErrorDeConfiguracion() {
        assertThrows(ConfiguracionNrusException.class, () -> motor.determinar(mes("0"), 7));
    }

    @Test
    void sinCategoriasVigentesEsErrorDeConfiguracion() {
        PeriodoMensual diciembre2025 = PeriodoMensual.nuevo(YearMonth.of(2025, 12));
        assertThrows(ConfiguracionNrusException.class, () -> motor.determinar(diciembre2025, DIGITO));
    }

    @Test
    void digitoFueraDeRango() {
        assertThrows(IllegalArgumentException.class, () -> motor.determinar(mes("0"), 10));
    }

    @Test
    void avisaTopeAnualConElAcumuladoDelAnio() {
        // Tope 96 000 × 0,80 = 76 800. Ocho meses previos con 4 800 de ventas = 38 400 + septiembre.
        List<PeriodoMensual> anio = new ArrayList<>();
        for (int m = 1; m <= 8; m++) {
            anio.add(new PeriodoMensual(YearMonth.of(2026, m), Montos.de("4800.00"), EstadoPeriodo.CERRADO,
                    List.of()));
        }
        assertFalse(motor.determinar(mes("4800.00"), DIGITO, anio).avisoTopeAnual());

        for (int m = 1; m <= 8; m++) {
            anio.set(m - 1, new PeriodoMensual(YearMonth.of(2026, m), Montos.de("9000.00"), EstadoPeriodo.CERRADO,
                    List.of()));
        }
        assertTrue(motor.determinar(mes("4800.00"), DIGITO, anio).avisoTopeAnual());
    }

    @Test
    void elTopeAnualIgnoraOtrosAniosYElMismoMes() {
        List<PeriodoMensual> otros = List.of(
                new PeriodoMensual(YearMonth.of(2025, 12), Montos.de("90000.00"), EstadoPeriodo.CERRADO, List.of()),
                new PeriodoMensual(Datos.SEPTIEMBRE, Montos.de("90000.00"), EstadoPeriodo.ABIERTO, List.of()));
        assertFalse(motor.determinar(mes("100.00"), DIGITO, otros).avisoTopeAnual());
    }

    @Test
    void conUnaSolaCategoriaElLimiteExactoEsLimite100() {
        ParametrosNrus base = Datos.parametros();
        ParametrosNrus una = new ParametrosNrus("prueba", base.vigenteDesde(), base.umbralAviso(), base.topeAnual(),
                List.of(base.categorias().get(0)), base.cronograma());
        Determinacion d = new MotorReglasNRUS(una).determinar(mes("5000.00"), DIGITO);
        assertEquals(NivelAlerta.LIMITE_100, d.nivelAlerta());
    }
}
