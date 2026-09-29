package pe.facturass20.ui.inicio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import pe.facturass20.dominio.modelo.CategoriaNRUS;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.CronogramaVencimiento;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.Emisor;
import pe.facturass20.dominio.modelo.EstadoPeriodo;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.Moneda;
import pe.facturass20.dominio.modelo.NivelAlerta;
import pe.facturass20.dominio.modelo.OrigenRegistro;
import pe.facturass20.dominio.modelo.ParametrosNrus;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.reglas.Montos;
import pe.facturass20.dominio.reglas.MotorReglasNRUS;

/** El mes del prototipo de P04: compras S/ 4 120,50, ventas S/ 3 900,00, categoría 1 al 82 %. */
public class ResumenInicioTest {

    private static final YearMonth SEPTIEMBRE = YearMonth.of(2026, 9);
    private static final LocalDate HOY = LocalDate.of(2026, 9, 27);
    private static final Contribuyente ROSA = new Contribuyente("10456789124", "Bodega El Progreso",
            "Rosa Huamán Quispe", LocalDate.of(2026, 9, 1));
    private static final ParametrosNrus PARAMETROS = new ParametrosNrus("prueba", LocalDate.of(2026, 1, 1),
            new BigDecimal("0.80"), Montos.de("96000"),
            Arrays.asList(new CategoriaNRUS(1, Montos.de("5000"), Montos.de("20")),
                    new CategoriaNRUS(2, Montos.de("8000"), Montos.de("50"))),
            Collections.singletonList(new CronogramaVencimiento(2026, 9, 4, LocalDate.of(2026, 10, 15))));

    private static long siguienteId = 1;

    private static FacturaCompra factura(String proveedor, String numero, int dia, String importe) {
        FacturaCompra factura = FacturaCompra.nueva(new Emisor("20100070970", proveedor), "F001", numero,
                SEPTIEMBRE.atDay(dia), Moneda.PEN, Montos.de(importe), OrigenRegistro.IA);
        factura.asignarId(siguienteId++);
        return factura;
    }

    private static PeriodoMensual mes(String ventas, FacturaCompra... facturas) {
        return new PeriodoMensual(SEPTIEMBRE, ventas == null ? null : Montos.de(ventas), EstadoPeriodo.ABIERTO,
                Arrays.asList(facturas));
    }

    private static ResumenInicio resumen(PeriodoMensual periodo) {
        Determinacion determinacion = new MotorReglasNRUS(PARAMETROS).determinar(periodo, ROSA.ultimoDigitoRuc());
        return ResumenInicio.de(ROSA, periodo, determinacion, PARAMETROS, HOY);
    }

    @Test
    public void mesDelPrototipo() {
        ResumenInicio r = resumen(mes("3900",
                factura("Otra S.A.", "100", 5, "1672.10"),
                factura("Molinos del Sur S.A.", "1284", 18, "612.00"),
                factura("Comercial Lima Norte S.A.C.", "915", 20, "386.40"),
                factura("Distribuidora Andina S.A.C.", "4821", 22, "1450.00")));

        assertEquals("Rosa", r.primerNombre());
        assertEquals(0, Montos.de("4120.50").compareTo(r.compras()));
        assertEquals(4, r.numeroFacturas());
        assertEquals(NivelAlerta.AVISO_80, r.nivel());
        assertEquals(Integer.valueOf(1), r.categoria());
        assertEquals(0, Montos.de("20").compareTo(r.cuota()));
        assertEquals(82, r.porcentaje());
        assertEquals(0, Montos.de("879.50").compareTo(r.restante()));
        assertEquals(Integer.valueOf(2), r.siguienteCategoria());
        assertFalse(r.ventasDeterminan());
        assertEquals(LocalDate.of(2026, 10, 15), r.vencimiento());
        assertEquals(18, r.diasParaVencer());
        assertTrue(r.hayAvisos());

        List<FacturaCompra> ultimas = r.ultimas();
        assertEquals(3, ultimas.size());
        assertEquals("Distribuidora Andina S.A.C.", ultimas.get(0).emisor().razonSocial());
        assertEquals("Comercial Lima Norte S.A.C.", ultimas.get(1).emisor().razonSocial());
        assertEquals("Molinos del Sur S.A.", ultimas.get(2).emisor().razonSocial());
    }

    @Test
    public void mesVacioSinVentas() {
        ResumenInicio r = resumen(mes(null));
        assertEquals(0, r.numeroFacturas());
        assertEquals(0, r.porcentaje());
        assertNull(r.ventas());
        assertEquals(NivelAlerta.NINGUNA, r.nivel());
        assertFalse(r.hayAvisos());
        assertTrue(r.ultimas().isEmpty());
        assertEquals(0, Montos.de("5000").compareTo(r.restante()));
    }

    @Test
    public void lasVentasMayoresDecidenLaCategoria() {
        ResumenInicio r = resumen(mes("6000", factura("A", "1", 3, "1000")));
        assertTrue(r.ventasDeterminan());
        assertEquals(Integer.valueOf(2), r.categoria());
        assertEquals(75, r.porcentaje());
        assertNull("la 2 es la última categoría", r.siguienteCategoria());
        assertEquals(0, Montos.de("2000").compareTo(r.restante()));
    }

    @Test
    public void fueraDelRegimen() {
        ResumenInicio r = resumen(mes("9000"));
        assertTrue(r.fueraDeRegimen());
        assertNull(r.categoria());
        assertEquals(0, Montos.de("8000").compareTo(r.limite()));
        assertEquals(112, r.porcentaje());
        assertEquals(100, r.progreso());
        assertEquals(0, Montos.CERO.compareTo(r.restante()));
    }

    @Test
    public void facturasDelMismoDiaVaPrimeroLaUltimaRegistrada() {
        FacturaCompra primera = factura("Primera", "1", 10, "10");
        FacturaCompra segunda = factura("Segunda", "2", 10, "10");
        ResumenInicio r = resumen(mes(null, primera, segunda));
        assertEquals("Segunda", r.ultimas().get(0).emisor().razonSocial());
    }

    @Test
    public void vencimientoPasado() {
        PeriodoMensual periodo = mes(null);
        Determinacion determinacion = new MotorReglasNRUS(PARAMETROS).determinar(periodo, ROSA.ultimoDigitoRuc());
        ResumenInicio r = ResumenInicio.de(ROSA, periodo, determinacion, PARAMETROS, LocalDate.of(2026, 10, 17));
        assertEquals(-2, r.diasParaVencer());
    }
}
