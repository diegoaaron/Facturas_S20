package pe.facturass20.dominio.casosuso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.facturass20.dominio.Datos;
import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.EstadoFactura;
import pe.facturass20.dominio.modelo.EstadoPeriodo;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.ImagenFactura;
import pe.facturass20.dominio.modelo.NivelAlerta;
import pe.facturass20.dominio.modelo.OrigenRegistro;
import pe.facturass20.dominio.modelo.ParametrosNrus;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;
import pe.facturass20.dominio.puertos.Reloj;
import pe.facturass20.dominio.reglas.Montos;
import pe.facturass20.dominio.reglas.ValidadorFecha;

/** Flujo de registro, ventas, anulación y cierre sobre repositorios en memoria. Hoy es 28/09/2026. */
class CasosUsoTest {

    private static final Reloj RELOJ = Reloj.deClock(Clock.fixed(
            ZonedDateTime.of(2026, 9, 28, 10, 0, 0, 0, Reloj.ZONA_LIMA).toInstant(), Reloj.ZONA_LIMA));

    private BaseEnMemoria base;
    private DeterminarCategoria determinar;
    private VerificarFactura verificar;
    private RegistrarFactura registrar;
    private RegistrarVentas ventas;
    private AnularFactura anular;
    private CerrarPeriodo cerrar;

    @BeforeEach
    void preparar() {
        base = new BaseEnMemoria();
        base.guardar(new Contribuyente(Datos.RUC_CONTRIBUYENTE, "Bodega Rosita", "Rosa Quispe",
                LocalDate.of(2026, 9, 1)));
        determinar = new DeterminarCategoria(base, base, base);
        verificar = new VerificarFactura(base, base, RELOJ);
        registrar = new RegistrarFactura(base, base, determinar, RELOJ);
        ventas = new RegistrarVentas(base, determinar);
        anular = new AnularFactura(base, base, determinar);
        cerrar = new CerrarPeriodo(base, determinar);
    }

    private static VerificarFactura.Datos datos(String numero, String fecha, String importe) {
        return new VerificarFactura.Datos("20601234565", "DISTRIBUIDORA ANDINA S.A.C.", "f001", numero, fecha,
                "PEN", importe);
    }

    private RegistrarFactura.Resultado registrarValida(String numero, String fecha, String importe) {
        VerificarFactura.Resultado verificacion = verificar.ejecutar(datos(numero, fecha, importe), OrigenRegistro.IA);
        assertTrue(verificacion.puedeGuardar(), () -> "errores: " + verificacion.errores());
        return registrar.ejecutar(verificacion.factura().orElseThrow(), null, null, false);
    }

    @Test
    void verificaYNormalizaUnaFacturaValida() {
        VerificarFactura.Resultado r = verificar.ejecutar(datos("004821", "22/09/2026", "1 450,00"), OrigenRegistro.IA);
        assertTrue(r.errores().isEmpty());
        assertTrue(r.puedeGuardar());
        assertFalse(r.requiereConfirmarPeriodo());
        assertEquals(ValidadorFecha.Resultado.DEL_MES, r.fecha().orElseThrow());
        FacturaCompra f = r.factura().orElseThrow();
        assertEquals("F001", f.serie());
        assertEquals(LocalDate.of(2026, 9, 22), f.fechaEmision());
        assertEquals(Montos.de("1450.00"), f.importeTotal());
        assertEquals(OrigenRegistro.IA, f.origen());
    }

    @Test
    void verificacionMarcaCadaCampoInvalido() {
        VerificarFactura.Datos malos = new VerificarFactura.Datos("20601234566", "ANDINA", "F001", "1",
                "2026-09-22", "PEN", "");
        VerificarFactura.Resultado r = verificar.ejecutar(malos, OrigenRegistro.MANUAL);
        assertEquals(List.of(CampoFactura.RUC, CampoFactura.IMPORTE_TOTAL), List.copyOf(r.errores().keySet()));
        assertTrue(r.factura().isEmpty());
        assertFalse(r.puedeGuardar());
    }

    @Test
    void fechaFuturaOMesCerradoSonErrores() {
        assertEquals("La fecha no puede ser posterior a hoy.", verificar.ejecutar(
                datos("1", "29/09/2026", "10"), OrigenRegistro.IA).errores().get(CampoFactura.FECHA_EMISION));

        ventas.ejecutar(YearMonth.of(2026, 8), Montos.de("100"));
        cerrar.ejecutar(YearMonth.of(2026, 8));
        assertEquals("El mes de esta factura ya está cerrado.", verificar.ejecutar(
                datos("1", "20/08/2026", "10"), OrigenRegistro.IA).errores().get(CampoFactura.FECHA_EMISION));
    }

    @Test
    void registraYRecalcula() {
        ImagenFactura imagen = new ImagenFactura("x.jpg.enc", 150, LocalDateTime.of(2026, 9, 28, 9, 0));
        ResultadoExtraccion extraccion = new ResultadoExtraccion("1.0.0", 9000,
                List.of(CampoExtraido.leido(CampoFactura.IMPORTE_TOTAL, "4120.50", 0.9)));
        FacturaCompra factura = verificar.ejecutar(datos("4821", "2026-09-22", "4120.50"), OrigenRegistro.IA)
                .factura().orElseThrow();

        RegistrarFactura.Resultado r = registrar.ejecutar(factura, imagen, extraccion, false);

        assertEquals(1L, r.factura().id());
        assertEquals(Montos.de("4120.50"), r.determinacion().totalAdquisiciones());
        assertEquals(NivelAlerta.AVISO_80, r.determinacion().nivelAlerta());
        assertEquals(LocalDate.of(2026, 10, 15), r.determinacion().fechaVencimiento());
        assertSame(imagen, base.ultimaImagen);
        assertSame(extraccion, base.ultimaExtraccion);
        assertEquals(r.determinacion(), base.buscarDeterminacion(Datos.SEPTIEMBRE).orElseThrow());
        assertEquals(1, base.buscar(Datos.SEPTIEMBRE).orElseThrow().facturas().size());
    }

    @Test
    void detectaElDuplicadoAlVerificarYAlRegistrar() {
        RegistrarFactura.Resultado primera = registrarValida("004821", "2026-09-22", "100");

        VerificarFactura.Resultado r = verificar.ejecutar(datos("4821", "2026-09-23", "200"), OrigenRegistro.IA);
        assertSame(primera.factura(), r.duplicado().orElseThrow());
        assertFalse(r.puedeGuardar());

        FacturaDuplicadaException e = assertThrows(FacturaDuplicadaException.class,
                () -> registrar.ejecutar(r.factura().orElseThrow(), null, null, false));
        assertSame(primera.factura(), e.existente());
    }

    @Test
    void mesAntiguoPideConfirmacion() {
        VerificarFactura.Resultado r = verificar.ejecutar(datos("9", "10/07/2026", "50"), OrigenRegistro.MANUAL);
        assertTrue(r.requiereConfirmarPeriodo());
        assertTrue(r.puedeGuardar());
        FacturaCompra factura = r.factura().orElseThrow();
        assertThrows(ReglaNegocioException.class, () -> registrar.ejecutar(factura, null, null, false));
        assertEquals(YearMonth.of(2026, 7), registrar.ejecutar(factura, null, null, true).determinacion().periodo());
    }

    @Test
    void registrarExigeContribuyente() {
        base.contribuyente = null;
        FacturaCompra factura = verificar.ejecutar(datos("1", "2026-09-22", "10"), OrigenRegistro.IA)
                .factura().orElseThrow();
        assertThrows(ReglaNegocioException.class, () -> registrar.ejecutar(factura, null, null, false));
    }

    @Test
    void lasVentasPuedenDeterminarLaCategoria() {
        registrarValida("1", "2026-09-10", "3000");
        Determinacion d = ventas.ejecutar(Datos.SEPTIEMBRE, Montos.de("5500"));
        assertEquals(2, d.categoria().codigo());
        assertEquals(Montos.de("5500.00"), d.montoDeterminante());
        assertEquals(Montos.de("3000.00"), d.totalAdquisiciones());
        assertThrows(ReglaNegocioException.class, () -> ventas.ejecutar(Datos.SEPTIEMBRE, Montos.de("-1")));
    }

    @Test
    void anularRecalculaElAcumulado() {
        registrarValida("1", "2026-09-10", "3734.10");
        long id = registrarValida("2", "2026-09-11", "386.40").factura().id();

        Determinacion d = anular.ejecutar(id, "La registré dos veces");

        assertEquals(Montos.de("3734.10"), d.totalAdquisiciones());
        assertEquals(EstadoFactura.ANULADA, base.buscarPorId(id).orElseThrow().estado());
        assertEquals(d, base.buscarDeterminacion(Datos.SEPTIEMBRE).orElseThrow());
        assertThrows(ReglaNegocioException.class, () -> anular.ejecutar(99, "No existe"));
    }

    @Test
    void cerrarCongelaLaDeterminacion() {
        registrarValida("1", "2026-09-10", "4500");
        assertThrows(ReglaNegocioException.class, () -> cerrar.ejecutar(Datos.SEPTIEMBRE));
        assertThrows(ReglaNegocioException.class, () -> cerrar.ejecutar(YearMonth.of(2026, 5)));

        ventas.ejecutar(Datos.SEPTIEMBRE, Montos.de("4000"));
        Determinacion alCerrar = cerrar.ejecutar(Datos.SEPTIEMBRE);
        assertEquals(EstadoPeriodo.CERRADO, base.buscar(Datos.SEPTIEMBRE).orElseThrow().estado());

        // Parámetros nuevos no cambian un mes cerrado (RF-22).
        ParametrosNrus p = base.parametros;
        base.parametros = new ParametrosNrus("2026.2", p.vigenteDesde(), p.umbralAviso(), p.topeAnual(),
                List.of(new pe.facturass20.dominio.modelo.CategoriaNRUS(1, Montos.de("3000"), Montos.de("99"))),
                p.cronograma());
        assertEquals(alCerrar, determinar.ejecutar(Datos.SEPTIEMBRE));

        long id = base.buscar(Datos.SEPTIEMBRE).orElseThrow().facturas().get(0).id();
        assertThrows(ReglaNegocioException.class, () -> anular.ejecutar(id, "Tarde"));
    }

    @Test
    void determinarCreaElMesSiNoExiste() {
        Determinacion d = determinar.ejecutar(Datos.SEPTIEMBRE);
        assertEquals(NivelAlerta.NINGUNA, d.nivelAlerta());
        assertTrue(base.buscar(Datos.SEPTIEMBRE).isPresent());
    }

    @Test
    void datosDesdeUnaLectura() {
        ResultadoExtraccion lectura = new ResultadoExtraccion("1.0.0", 1, List.of(
                CampoExtraido.leido(CampoFactura.RUC, "20601234565", 0.98),
                CampoExtraido.leido(CampoFactura.IMPORTE_TOTAL, "1450.00", 0.62)))
                .conValorFinal(CampoFactura.IMPORTE_TOTAL, "1540.00");
        VerificarFactura.Datos d = VerificarFactura.Datos.desde(lectura);
        assertEquals("20601234565", d.ruc());
        assertEquals("1540.00", d.importeTotal());
        assertEquals(null, d.serie());
    }
}
