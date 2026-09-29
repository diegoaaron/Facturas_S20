package pe.facturass20.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import pe.facturass20.dominio.Datos;
import pe.facturass20.dominio.reglas.Montos;

class PeriodoMensualTest {

    private final PeriodoMensual periodo = PeriodoMensual.nuevo(Datos.SEPTIEMBRE);

    @Test
    void mesNuevoAbiertoSinVentas() {
        assertTrue(periodo.estaAbierto());
        assertFalse(periodo.ventasRegistradas());
        assertEquals(Montos.CERO, periodo.totalVentas());
        assertEquals(Montos.CERO, periodo.totalAdquisiciones());
        assertEquals(2026, periodo.anio());
        assertEquals(9, periodo.mes());
    }

    @Test
    void sumaSoloLasVigentes() {
        periodo.agregarFactura(Datos.guardada(1, "1", "100.50"));
        periodo.agregarFactura(Datos.guardada(2, "2", "200.00"));
        periodo.anularFactura(2, "Duplicada en papel");
        assertEquals(Montos.de("100.50"), periodo.totalAdquisiciones());
        assertEquals(1, periodo.facturasVigentes().size());
        assertEquals(2, periodo.facturas().size());
        assertEquals(EstadoFactura.ANULADA, periodo.facturas().get(1).estado());
        assertEquals("Duplicada en papel", periodo.facturas().get(1).motivoAnulacion());
    }

    @Test
    void registraVentas() {
        periodo.registrarVentas(Montos.de("3900"));
        assertTrue(periodo.ventasRegistradas());
        assertEquals(Montos.de("3900.00"), periodo.totalVentas());
        assertThrows(ReglaNegocioException.class, () -> periodo.registrarVentas(Montos.de("-1")));
    }

    @Test
    void cerrarExigeVentasYLuegoNoSeEdita() {
        ReglaNegocioException sinVentas = assertThrows(ReglaNegocioException.class, periodo::cerrar);
        assertEquals("Antes de cerrar el mes, registre sus ventas.", sinVentas.getMessage());

        periodo.agregarFactura(Datos.guardada(1, "1", "100.00"));
        periodo.registrarVentas(Montos.CERO);
        periodo.cerrar();
        assertEquals(EstadoPeriodo.CERRADO, periodo.estado());
        assertThrows(ReglaNegocioException.class, () -> periodo.registrarVentas(Montos.de("1")));
        assertThrows(ReglaNegocioException.class, () -> periodo.agregarFactura(Datos.factura("2", "1.00")));
        assertThrows(ReglaNegocioException.class, () -> periodo.anularFactura(1, "x"));
        assertThrows(ReglaNegocioException.class, periodo::cerrar);
    }

    @Test
    void rechazaFacturasQueNoCorresponden() {
        assertThrows(IllegalArgumentException.class,
                () -> periodo.agregarFactura(Datos.factura("1", "10.00", LocalDate.of(2026, 8, 31))));
        FacturaCompra dolares = FacturaCompra.nueva(new Emisor(Datos.RUC_EMISOR, "X"), "F001", "9",
                LocalDate.of(2026, 9, 1), Moneda.USD, Montos.de("10"), OrigenRegistro.MANUAL);
        assertThrows(ReglaNegocioException.class, () -> periodo.agregarFactura(dolares));
        periodo.agregarFactura(Datos.factura("0015", "10.00"));
        assertThrows(ReglaNegocioException.class, () -> periodo.agregarFactura(Datos.factura("15", "10.00")));
    }

    @Test
    void anularExigeMotivoYFacturaVigente() {
        periodo.agregarFactura(Datos.guardada(1, "1", "100.00"));
        assertThrows(ReglaNegocioException.class, () -> periodo.anularFactura(1, " "));
        assertThrows(ReglaNegocioException.class, () -> periodo.anularFactura(99, "No existe"));
        periodo.anularFactura(1, "Error");
        assertThrows(ReglaNegocioException.class, () -> periodo.anularFactura(1, "Otra vez"));
    }
}
