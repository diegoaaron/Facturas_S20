package pe.facturass20.dominio.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.facturass20.dominio.Datos;
import pe.facturass20.dominio.modelo.EstadoPeriodo;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.PeriodoMensual;

class DetectorDuplicadosTest {

    @Test
    void mismaClaveEsDuplicadoAunqueCambienLosCerosDeRelleno() {
        FacturaCompra existente = Datos.guardada(7, "004821", "100.00");
        FacturaCompra nueva = Datos.factura("4821", "250.00");
        assertEquals(existente, DetectorDuplicados.buscarDuplicado(nueva, List.of(existente)).orElseThrow());
    }

    @Test
    void otroNumeroNoEsDuplicado() {
        FacturaCompra existente = Datos.guardada(7, "004821", "100.00");
        assertTrue(DetectorDuplicados.buscarDuplicado(Datos.factura("4822", "100.00"), List.of(existente)).isEmpty());
    }

    @Test
    void unaAnuladaNoCuenta() {
        FacturaCompra existente = Datos.guardada(7, "004821", "100.00");
        new PeriodoMensual(YearMonth.of(2026, 9), null, EstadoPeriodo.ABIERTO, List.of(existente))
                .anularFactura(7, "Mal leída");
        assertTrue(DetectorDuplicados.buscarDuplicado(Datos.factura("4821", "100.00"), List.of(existente)).isEmpty());
    }

    @Test
    void laMismaFacturaNoEsSuPropioDuplicado() {
        FacturaCompra existente = Datos.guardada(7, "004821", "100.00");
        assertTrue(DetectorDuplicados.buscarDuplicado(existente, List.of(existente)).isEmpty());
    }
}
