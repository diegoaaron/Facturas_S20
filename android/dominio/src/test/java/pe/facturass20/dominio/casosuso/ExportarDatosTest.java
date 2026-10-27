package pe.facturass20.dominio.casosuso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.facturass20.dominio.Datos;
import pe.facturass20.dominio.modelo.CategoriaNRUS;
import pe.facturass20.dominio.modelo.DatosExportacion;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.EstadoPeriodo;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.NivelAlerta;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.dominio.modelo.ResumenExportacion;
import pe.facturass20.dominio.modelo.SolicitudExportacion;
import pe.facturass20.dominio.reglas.Montos;

/** Qué meses y determinaciones llegan al exportador según la solicitud de P20. */
class ExportarDatosTest {

    private BaseEnMemoria base;
    private DatosExportacion recibidos;
    private ExportarDatos exportar;

    @BeforeEach
    void preparar() {
        base = new BaseEnMemoria();
        exportar = new ExportarDatos(base, (datos, destino) -> {
            recibidos = datos;
            return new ResumenExportacion(0, 0, 0, Montos.CERO);
        });
        // Agosto con una factura y su determinación; septiembre solo con ventas; octubre vacío; 2025 aparte.
        mes(YearMonth.of(2026, 8), null, List.of(Datos.factura("1", "100.00", LocalDate.of(2026, 8, 3))));
        base.determinaciones.put(YearMonth.of(2026, 8), determinacion(YearMonth.of(2026, 8)));
        mes(Datos.SEPTIEMBRE, Montos.de("500.00"), List.of());
        mes(YearMonth.of(2026, 10), null, List.of());
        mes(YearMonth.of(2025, 12), null, List.of(Datos.factura("2", "50.00", LocalDate.of(2025, 12, 1))));
    }

    private void mes(YearMonth periodo, BigDecimal ventas, List<FacturaCompra> facturas) {
        base.periodos.put(periodo, new PeriodoMensual(periodo, ventas, EstadoPeriodo.ABIERTO, facturas));
    }

    private static Determinacion determinacion(YearMonth periodo) {
        return new Determinacion(periodo, Montos.de("100.00"), Montos.de("100.00"), new CategoriaNRUS(1, Montos.de("5000.00"), Montos.de("20.00")),
                Montos.de("20.00"), periodo.plusMonths(1).atDay(15), NivelAlerta.NINGUNA, false);
    }

    @Test
    void elAnioLlevaSoloLosMesesConDatosDeEseAnio() throws IOException {
        exportar.ejecutar(new SolicitudExportacion(2026, null, true, true), new ByteArrayOutputStream());
        assertEquals(List.of(YearMonth.of(2026, 8), Datos.SEPTIEMBRE),
                recibidos.periodos().stream().map(PeriodoMensual::periodo).toList());
        assertEquals(1, recibidos.determinaciones().size());
        assertTrue(recibidos.determinaciones().containsKey(YearMonth.of(2026, 8)));
        assertEquals("facturas-s20_2026.zip", recibidos.nombreArchivo());
    }

    @Test
    void unMesLlevaSoloEseMes() throws IOException {
        exportar.ejecutar(new SolicitudExportacion(2026, 9, false, false), new ByteArrayOutputStream());
        assertEquals(List.of(Datos.SEPTIEMBRE), recibidos.periodos().stream().map(PeriodoMensual::periodo).toList());
        assertEquals("facturas-s20_2026-09.zip", recibidos.nombreArchivo());
    }

    @Test
    void unPeriodoSinDatosNoSeExporta() {
        assertThrows(ReglaNegocioException.class, () ->
                exportar.ejecutar(new SolicitudExportacion(2026, 10, true, true), new ByteArrayOutputStream()));
        assertThrows(ReglaNegocioException.class, () ->
                exportar.ejecutar(new SolicitudExportacion(2024, null, true, true), new ByteArrayOutputStream()));
    }
}
