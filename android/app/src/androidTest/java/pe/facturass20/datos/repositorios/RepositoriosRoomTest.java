package pe.facturass20.datos.repositorios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.entidades.CampoExtraidoEntity;
import pe.facturass20.datos.entidades.ImagenFacturaEntity;
import pe.facturass20.datos.parametros.CargadorParametrosNrus;
import pe.facturass20.dominio.casosuso.AnularFactura;
import pe.facturass20.dominio.casosuso.CerrarPeriodo;
import pe.facturass20.dominio.casosuso.DeterminarCategoria;
import pe.facturass20.dominio.casosuso.RegistrarFactura;
import pe.facturass20.dominio.casosuso.RegistrarVentas;
import pe.facturass20.dominio.casosuso.VerificarFactura;
import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.CategoriaNRUS;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.EstadoFactura;
import pe.facturass20.dominio.modelo.EstadoPeriodo;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.ImagenFactura;
import pe.facturass20.dominio.modelo.NivelAlerta;
import pe.facturass20.dominio.modelo.OrigenRegistro;
import pe.facturass20.dominio.modelo.ParametrosNrus;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;
import pe.facturass20.dominio.puertos.Reloj;
import pe.facturass20.dominio.reglas.Montos;

/**
 * Repositorios Room sobre una base SQLCipher en memoria, usados a través de los casos de uso del dominio.
 * Hoy es 28/09/2026.
 */
@RunWith(AndroidJUnit4.class)
public class RepositoriosRoomTest {

    private static final YearMonth SEPTIEMBRE = YearMonth.of(2026, 9);
    private static final Reloj RELOJ = Reloj.deClock(Clock.fixed(
            ZonedDateTime.of(2026, 9, 28, 10, 0, 0, 0, Reloj.ZONA_LIMA).toInstant(), Reloj.ZONA_LIMA));

    private BaseDatosFacturas db;
    private CargadorParametrosNrus cargador;
    private ContribuyenteRepositorioRoom contribuyentes;
    private PeriodoRepositorioRoom periodos;
    private FacturaRepositorioRoom facturas;
    private ParametrosRepositorioRoom parametros;
    private VerificarFactura verificar;
    private RegistrarFactura registrar;
    private RegistrarVentas ventas;
    private AnularFactura anular;
    private CerrarPeriodo cerrar;

    @Before
    public void abrir() {
        Context contexto = ApplicationProvider.getApplicationContext();
        byte[] clave = new byte[32];
        new SecureRandom().nextBytes(clave);
        db = BaseDatosFacturas.enMemoria(contexto, clave);
        cargador = new CargadorParametrosNrus(contexto, db);
        assertTrue(cargador.cargarSiHaceFalta());

        contribuyentes = new ContribuyenteRepositorioRoom(db);
        periodos = new PeriodoRepositorioRoom(db, RELOJ);
        facturas = new FacturaRepositorioRoom(db, periodos, RELOJ);
        parametros = new ParametrosRepositorioRoom(db);
        contribuyentes.guardar(new Contribuyente("10456789124", "Bodega Rosita", "Rosa Quispe",
                LocalDate.of(2026, 9, 1)));

        DeterminarCategoria determinar = new DeterminarCategoria(periodos, contribuyentes, parametros);
        verificar = new VerificarFactura(periodos, facturas, RELOJ);
        registrar = new RegistrarFactura(periodos, facturas, determinar, RELOJ);
        ventas = new RegistrarVentas(periodos, determinar);
        anular = new AnularFactura(periodos, facturas, determinar);
        cerrar = new CerrarPeriodo(periodos, determinar);
    }

    @After
    public void cerrarBase() {
        db.close();
    }

    private FacturaCompra verificada(String numero, String importe) {
        VerificarFactura.Resultado r = verificar.ejecutar(new VerificarFactura.Datos("20601234565",
                "DISTRIBUIDORA ANDINA S.A.C.", "F001", numero, "22/09/2026", "PEN", importe), OrigenRegistro.IA);
        assertTrue("errores: " + r.errores() + ", duplicado: " + r.duplicado(), r.puedeGuardar());
        return r.factura().get();
    }

    private static void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, Montos.de(esperado).compareTo(real));
    }

    @Test
    public void cargaLosParametrosDelAssetUnaSolaVez() {
        assertFalse(cargador.cargarSiHaceFalta());
        ParametrosNrus p = parametros.vigentes();
        assertEquals("2026.0-provisional", p.version());
        assertEquals(Arrays.asList(1, 2), Arrays.asList(p.categorias().get(0).codigo(), p.categorias().get(1).codigo()));
        assertMonto("5000.00", p.categorias().get(0).limiteMensual());
        assertMonto("0.80", p.umbralAviso());
        assertMonto("96000.00", p.topeAnual());
        assertEquals(120, p.cronograma().size());
    }

    @Test
    public void registraYReleeLaFacturaConTodosSusDatos() {
        ImagenFactura imagen = new ImagenFactura("00000000-0000-0000-0000-000000000000.jpg.enc", 142.5,
                LocalDateTime.of(2026, 9, 28, 9, 30));
        ResultadoExtraccion extraccion = new ResultadoExtraccion("1.0.0", 9500, Arrays.asList(
                CampoExtraido.leido(CampoFactura.RUC, "20601234565", 0.98),
                CampoExtraido.leido(CampoFactura.IMPORTE_TOTAL, "4102.50", 0.62).conValorFinal("4120.50")));

        RegistrarFactura.Resultado r = registrar.ejecutar(verificada("004821", "4120,50"), imagen, extraccion, false);

        PeriodoMensual releido = periodos.buscar(SEPTIEMBRE).get();
        assertEquals(1, releido.facturas().size());
        FacturaCompra f = releido.facturas().get(0);
        assertEquals(r.factura().id(), f.id());
        assertEquals("004821", f.numero());
        assertEquals("DISTRIBUIDORA ANDINA S.A.C.", f.emisor().razonSocial());
        assertEquals(new BigDecimal("4120.50"), f.importeTotal());
        assertEquals(LocalDate.of(2026, 9, 22), f.fechaEmision());
        assertEquals(OrigenRegistro.IA, f.origen());
        assertFalse(releido.ventasRegistradas());

        Determinacion d = periodos.buscarDeterminacion(SEPTIEMBRE).get();
        assertEquals(r.determinacion(), d);
        assertEquals(NivelAlerta.AVISO_80, d.nivelAlerta());
        assertEquals(new CategoriaNRUS(1, Montos.de("5000"), Montos.de("20")), d.categoria());
        assertEquals(LocalDate.of(2026, 10, 15), d.fechaVencimiento());

        ImagenFacturaEntity filaImagen = db.facturaDao().buscarImagen(f.id());
        assertEquals(imagen.ruta(), filaImagen.rutaCifrada);
        assertEquals(142.5, filaImagen.nitidez, 0.0);
        List<CampoExtraidoEntity> campos = db.facturaDao().listarCampos(f.id());
        assertEquals(2, campos.size());
        assertFalse(campos.get(0).corregido);
        assertTrue(campos.get(1).corregido);
        assertEquals("4102.50", campos.get(1).valorLeido);
        assertNull(campos.get(1).idModelo);
        assertEquals("4821", db.facturaDao().buscarPorId(f.id()).factura.numeroNormalizado);
    }

    @Test
    public void detectaElDuplicadoGuardadoSinImportarLosCeros() {
        registrar.ejecutar(verificada("004821", "100"), null, null, false);
        VerificarFactura.Resultado r = verificar.ejecutar(new VerificarFactura.Datos("20601234565",
                "ANDINA", "F001", "4821", "23/09/2026", "PEN", "100"), OrigenRegistro.MANUAL);
        assertTrue(r.duplicado().isPresent());
        assertEquals("004821", r.duplicado().get().numero());
    }

    @Test
    public void unaFacturaAnuladaSePuedeVolverARegistrar() {
        long id = registrar.ejecutar(verificada("15", "386.40"), null, null, false).factura().id();
        Determinacion d = anular.ejecutar(id, "Mal leída");
        assertMonto("0", d.totalAdquisiciones());

        FacturaCompra anulada = facturas.buscarPorId(id).get();
        assertEquals(EstadoFactura.ANULADA, anulada.estado());
        assertEquals("Mal leída", anulada.motivoAnulacion());

        registrar.ejecutar(verificada("15", "368.40"), null, null, false);
        assertMonto("368.40", periodos.buscarDeterminacion(SEPTIEMBRE).get().totalAdquisiciones());
        assertEquals(2, periodos.buscar(SEPTIEMBRE).get().facturas().size());
    }

    @Test
    public void distingueVentasSinRegistrarDeVentasEnCero() {
        ventas.ejecutar(SEPTIEMBRE, Montos.CERO);
        PeriodoMensual p = periodos.buscar(SEPTIEMBRE).get();
        assertTrue(p.ventasRegistradas());
        assertMonto("0", p.totalVentas());

        Determinacion d = ventas.ejecutar(SEPTIEMBRE, Montos.de("5500"));
        assertEquals(2, d.categoria().codigo());
        assertEquals(NivelAlerta.LIMITE_100, periodos.buscarDeterminacion(SEPTIEMBRE).get().nivelAlerta());
    }

    @Test
    public void cerrarElMesLoCongela() {
        registrar.ejecutar(verificada("1", "1000"), null, null, false);
        ventas.ejecutar(SEPTIEMBRE, Montos.de("900"));
        cerrar.ejecutar(SEPTIEMBRE);
        assertEquals(EstadoPeriodo.CERRADO, periodos.buscar(SEPTIEMBRE).get().estado());
        assertEquals(1, periodos.listar().size());
        assertEquals(1, periodos.listarDelAnio(2026).size());
        assertTrue(periodos.listarDelAnio(2025).isEmpty());
    }

    @Test
    public void siAlgoFallaNoQuedaNadaAMedias() {
        FacturaCompra factura = verificada("77", "50");
        PeriodoMensual periodo = PeriodoMensual.nuevo(SEPTIEMBRE);
        periodo.agregarFactura(factura);
        // Una categoría que no existe en los parámetros hace fallar el último paso de la transacción.
        Determinacion invalida = new Determinacion(SEPTIEMBRE, Montos.de("50"), Montos.de("50"),
                new CategoriaNRUS(99, Montos.de("1"), Montos.de("1")), Montos.de("1"), LocalDate.of(2026, 10, 15),
                NivelAlerta.NINGUNA, false);

        assertThrows(IllegalStateException.class,
                () -> facturas.registrar(periodo, factura, null, null, invalida));

        assertFalse(periodos.buscar(SEPTIEMBRE).isPresent());
        assertFalse(facturas.buscarEmisor("20601234565").isPresent());
        assertTrue(facturas.buscarPorEmisorYSerie("20601234565", "F001").isEmpty());
    }

    @Test
    public void elContribuyenteSeActualizaSinDuplicarseNiPerderElPin() {
        db.contribuyenteDao().guardarPin(db.contribuyenteDao().obtener().idContribuyente, "hash", "sal");
        contribuyentes.guardar(new Contribuyente("20601234565", "Bodega Rosita E.I.R.L.", "Rosa Quispe",
                LocalDate.of(2026, 9, 1)));
        Contribuyente c = contribuyentes.obtener().get();
        assertEquals("20601234565", c.ruc());
        assertEquals(5, db.contribuyenteDao().obtener().ultimoDigito);
        assertEquals("hash", db.contribuyenteDao().obtener().pinHash);
        assertNotNull(db.contribuyenteDao().obtener());
    }
}
