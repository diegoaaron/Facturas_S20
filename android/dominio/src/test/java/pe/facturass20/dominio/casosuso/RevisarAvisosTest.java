package pe.facturass20.dominio.casosuso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.facturass20.dominio.Datos;
import pe.facturass20.dominio.modelo.Aviso;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.EstadoPeriodo;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.TipoAviso;
import pe.facturass20.dominio.puertos.AvisoRepositorio;
import pe.facturass20.dominio.puertos.Reloj;
import pe.facturass20.dominio.reglas.Montos;

/**
 * Qué notificaciones tocan cada día. El RUC de prueba termina en 4: la declaración de setiembre vence el
 * 15/10/2026 (cronograma de {@link Datos#parametros()}).
 */
class RevisarAvisosTest {

    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);
    private static final RevisarAvisos.Preferencias TODAS = RevisarAvisos.Preferencias.todas();

    private BaseEnMemoria base;
    private DeterminarCategoria determinar;
    private final List<Aviso> mandados = new ArrayList<>();
    private final AvisoRepositorio avisos = new AvisoRepositorio() {
        @Override
        public List<Aviso> listar(YearMonth periodo) {
            return mandados.stream().filter(a -> a.periodo().equals(periodo)).toList();
        }

        @Override
        public void registrar(Aviso aviso) {
            mandados.add(aviso);
        }
    };

    @BeforeEach
    void preparar() {
        base = new BaseEnMemoria();
        base.guardar(new Contribuyente(Datos.RUC_CONTRIBUYENTE, "Bodega Rosita", "Rosa Quispe",
                LocalDate.of(2026, 1, 1)));
        determinar = new DeterminarCategoria(base, base, base);
    }

    private List<RevisarAvisos.Pendiente> revisar(LocalDate hoy, RevisarAvisos.Preferencias preferencias) {
        Reloj reloj = Reloj.deClock(Clock.fixed(hoy.atTime(9, 0).atZone(Reloj.ZONA_LIMA).toInstant(), Reloj.ZONA_LIMA));
        return new RevisarAvisos(base, base, determinar, avisos, reloj).ejecutar(preferencias);
    }

    private static List<TipoAviso> tipos(List<RevisarAvisos.Pendiente> pendientes) {
        return pendientes.stream().map(p -> p.aviso().tipo()).toList();
    }

    /** Un mes abierto con compras por ese monto y su determinación guardada. */
    private void compras(YearMonth mes, String importe, BigDecimal ventas) {
        List<FacturaCompra> facturas = List.of(Datos.factura("1", importe, mes.atDay(3)));
        base.periodos.put(mes, new PeriodoMensual(mes, ventas, EstadoPeriodo.ABIERTO, facturas));
        determinar.ejecutar(mes);
    }

    @Test
    void recuerdaElVencimientoCincoDiasAntesUnaSolaVez() {
        List<RevisarAvisos.Pendiente> pendientes = revisar(LocalDate.of(2026, 10, 10), TODAS);

        assertEquals(List.of(TipoAviso.VENCIMIENTO), tipos(pendientes));
        assertEquals(Datos.SEPTIEMBRE, pendientes.get(0).aviso().periodo());
        assertEquals(LocalDate.of(2026, 10, 10), pendientes.get(0).aviso().fechaProgramada());
        assertEquals(5, pendientes.get(0).diasParaVencer(LocalDate.of(2026, 10, 10)));
        assertTrue(revisar(LocalDate.of(2026, 10, 10), TODAS).isEmpty());
        assertTrue(revisar(LocalDate.of(2026, 10, 12), TODAS).isEmpty());
    }

    @Test
    void elDiaAntesMandaElSegundoYSiSePasoElPrimeroSoloEseUno() {
        List<RevisarAvisos.Pendiente> pendientes = revisar(LocalDate.of(2026, 10, 14), TODAS);

        assertEquals(1, pendientes.size());
        assertEquals(LocalDate.of(2026, 10, 14), pendientes.get(0).aviso().fechaProgramada());
        assertTrue(revisar(LocalDate.of(2026, 10, 15), TODAS).isEmpty());
    }

    @Test
    void despuesDelVencimientoOSinPreferenciaNoAvisa() {
        assertTrue(revisar(LocalDate.of(2026, 10, 16), TODAS).isEmpty());
        assertTrue(revisar(LocalDate.of(2026, 10, 9), TODAS).isEmpty());
        assertTrue(revisar(LocalDate.of(2026, 10, 10),
                new RevisarAvisos.Preferencias(true, true, true, false, true)).isEmpty());
    }

    @Test
    void sinNegocioConfiguradoOAntesDelAltaNoAvisa() {
        base.contribuyente = new Contribuyente(Datos.RUC_CONTRIBUYENTE, "Bodega Rosita", "Rosa Quispe",
                LocalDate.of(2026, 10, 2));
        assertTrue(revisar(LocalDate.of(2026, 10, 10), TODAS).isEmpty());
        base.contribuyente = null;
        assertTrue(revisar(LocalDate.of(2026, 10, 10), TODAS).isEmpty());
    }

    @Test
    void avisaEl80YLuegoEl100UnaVezPorMes() {
        compras(OCTUBRE, "4100.00", null);
        assertEquals(List.of(TipoAviso.TOPE_80), tipos(revisar(LocalDate.of(2026, 10, 20), TODAS)));
        assertTrue(revisar(LocalDate.of(2026, 10, 21), TODAS).isEmpty());

        compras(OCTUBRE, "5600.00", null);
        assertEquals(List.of(TipoAviso.TOPE_100), tipos(revisar(LocalDate.of(2026, 10, 22), TODAS)));
        assertTrue(revisar(LocalDate.of(2026, 10, 23), TODAS).isEmpty());
    }

    @Test
    void conEl80DesactivadoNoAvisa() {
        compras(OCTUBRE, "4100.00", null);
        assertTrue(revisar(LocalDate.of(2026, 10, 20),
                new RevisarAvisos.Preferencias(false, true, true, true, true)).isEmpty());
    }

    @Test
    void avisaDelTopeAnualUnaVezPorAnio() {
        // 8 000 de ventas cada mes hasta octubre: 80 000 en el año, más del 80 % del tope de 96 000.
        for (int mes = 1; mes <= 10; mes++) {
            compras(YearMonth.of(2026, mes), "100.00", Montos.de("8000.00"));
        }
        List<TipoAviso> tipos = tipos(revisar(LocalDate.of(2026, 10, 20), TODAS));
        assertTrue(tipos.contains(TipoAviso.TOPE_ANUAL), tipos::toString);

        compras(YearMonth.of(2026, 11), "100.00", Montos.de("8000.00"));
        assertTrue(!tipos(revisar(LocalDate.of(2026, 11, 20), TODAS)).contains(TipoAviso.TOPE_ANUAL));
    }
}
