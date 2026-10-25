package pe.facturass20.ui.facturas;

import static org.junit.Assert.assertEquals;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import pe.facturass20.dominio.modelo.Emisor;
import pe.facturass20.dominio.modelo.EstadoFactura;
import pe.facturass20.dominio.modelo.EstadoPeriodo;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.Moneda;
import pe.facturass20.dominio.modelo.OrigenRegistro;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.reglas.Montos;

/** Filtros, búsqueda y semanas de P11, con septiembre de 2026 (empieza martes y termina miércoles). */
public class ListadoFacturasTest {

    private static final YearMonth SEPTIEMBRE = YearMonth.of(2026, 9);
    private static long siguienteId = 1;

    private static FacturaCompra factura(String proveedor, String numero, int dia, String importe,
            OrigenRegistro origen, boolean anulada) {
        return new FacturaCompra(siguienteId++, new Emisor("20601234565", proveedor), "F001", numero,
                SEPTIEMBRE.atDay(dia), Moneda.PEN, Montos.de(importe), origen,
                anulada ? EstadoFactura.ANULADA : EstadoFactura.VIGENTE, anulada ? "Registrada por error" : null);
    }

    private static final PeriodoMensual MES = new PeriodoMensual(SEPTIEMBRE, null, EstadoPeriodo.ABIERTO, Arrays.asList(
            factura("Distribuidora Andina", "004655", 1, "214.00", OrigenRegistro.IA, false),
            factura("Lácteos San Juan", "002231", 12, "298.00", OrigenRegistro.IA, false),
            factura("Bebidas Rímac", "007712", 15, "540.10", OrigenRegistro.MANUAL, false),
            factura("Comercial Lima Norte", "000915", 20, "386.40", OrigenRegistro.IA, true),
            factura("Distribuidora Andina", "004821", 30, "1450.00", OrigenRegistro.IA, false)));

    @Test
    public void filtraPorOrigenYEstado() {
        assertEquals(4, ListadoFacturas.contar(MES, ListadoFacturas.Filtro.TODAS));
        assertEquals(3, ListadoFacturas.contar(MES, ListadoFacturas.Filtro.IA));
        assertEquals(1, ListadoFacturas.contar(MES, ListadoFacturas.Filtro.MANUALES));
        assertEquals(1, ListadoFacturas.contar(MES, ListadoFacturas.Filtro.ANULADAS));
    }

    @Test
    public void elTotalSoloSumaLasVigentes() {
        assertEquals(Montos.de("2502.10"), ListadoFacturas.total(MES));
    }

    @Test
    public void agrupaPorSemanaDeLaMasRecienteALaMasAntiguaRecortandoAlMes() {
        List<ListadoFacturas.Semana> semanas = ListadoFacturas.agrupar(MES, ListadoFacturas.Filtro.TODAS, "");
        List<String> rangos = new ArrayList<>();
        for (ListadoFacturas.Semana semana : semanas) {
            rangos.add(semana.desde() + "-" + semana.hasta() + ":" + semana.facturas().size());
        }
        // 30/09 (miércoles) cae en la semana del 28 al 30; el 1 (martes), en la del 1 al 6.
        assertEquals(Arrays.asList("28-30:1", "14-20:1", "7-13:1", "1-6:1"), rangos);
    }

    @Test
    public void buscaPorProveedorRucONumeroSinDistinguirMayusculas() {
        assertEquals(2, contar("andina"));
        assertEquals(1, contar("F001-007712"));
        assertEquals(4, contar("20601234565"));
        assertEquals(0, contar("molinos"));
    }

    private static int contar(String busqueda) {
        int total = 0;
        for (ListadoFacturas.Semana semana : ListadoFacturas.agrupar(MES, ListadoFacturas.Filtro.TODAS, busqueda)) {
            total += semana.facturas().size();
        }
        return total;
    }
}
