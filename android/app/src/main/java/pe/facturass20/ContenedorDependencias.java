package pe.facturass20;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.cifrado.AlmacenImagenes;
import pe.facturass20.datos.cifrado.GestorClaves;
import pe.facturass20.datos.cifrado.GestorPin;
import pe.facturass20.datos.parametros.CargadorParametrosNrus;
import pe.facturass20.datos.repositorios.ContribuyenteRepositorioRoom;
import pe.facturass20.datos.repositorios.FacturaRepositorioRoom;
import pe.facturass20.datos.repositorios.ParametrosRepositorioRoom;
import pe.facturass20.datos.repositorios.PeriodoRepositorioRoom;
import pe.facturass20.dominio.casosuso.AnularFactura;
import pe.facturass20.dominio.casosuso.CerrarPeriodo;
import pe.facturass20.dominio.casosuso.DeterminarCategoria;
import pe.facturass20.dominio.casosuso.RegistrarFactura;
import pe.facturass20.dominio.casosuso.RegistrarVentas;
import pe.facturass20.dominio.casosuso.VerificarFactura;
import pe.facturass20.dominio.puertos.ContribuyenteRepositorio;
import pe.facturass20.dominio.puertos.FacturaRepositorio;
import pe.facturass20.dominio.puertos.ParametrosRepositorio;
import pe.facturass20.dominio.puertos.PeriodoRepositorio;
import pe.facturass20.dominio.puertos.Reloj;

/**
 * Inyección de dependencias manual (documentación técnica §3.1): crea una sola vez los adaptadores
 * que implementan los puertos del dominio y los entrega a los ViewModels.
 *
 * <p>La base de datos y sus repositorios se crean la primera vez que se piden (la clave sale del
 * Keystore), siempre desde un hilo de fondo. Faltan por agregar el extractor de facturas (falso primero,
 * Gemma después) y el exportador de datos.</p>
 */
public final class ContenedorDependencias {

    private final Context contexto;
    private final ExecutorService ejecutor = Executors.newFixedThreadPool(2);
    private final Reloj reloj = Reloj.sistema();
    private Adaptadores adaptadores;

    ContenedorDependencias(Context contexto) {
        this.contexto = contexto.getApplicationContext();
    }

    /** Hilos de fondo para la base de datos, la inferencia, el PDF y la exportación; nunca el hilo de UI. */
    public ExecutorService ejecutor() {
        return ejecutor;
    }

    public Reloj reloj() {
        return reloj;
    }

    public BaseDatosFacturas baseDatos() {
        return adaptadores().base;
    }

    public ContribuyenteRepositorio contribuyentes() {
        return adaptadores().contribuyentes;
    }

    public PeriodoRepositorio periodos() {
        return adaptadores().periodos;
    }

    public FacturaRepositorio facturas() {
        return adaptadores().facturas;
    }

    public ParametrosRepositorio parametros() {
        return adaptadores().parametros;
    }

    public AlmacenImagenes almacenImagenes() {
        return adaptadores().imagenes;
    }

    public GestorPin gestorPin() {
        return adaptadores().pin;
    }

    public CargadorParametrosNrus cargadorParametros() {
        return adaptadores().cargador;
    }

    public DeterminarCategoria determinarCategoria() {
        return adaptadores().determinarCategoria;
    }

    public VerificarFactura verificarFactura() {
        return new VerificarFactura(periodos(), facturas(), reloj);
    }

    public RegistrarFactura registrarFactura() {
        return new RegistrarFactura(periodos(), facturas(), determinarCategoria(), reloj);
    }

    public RegistrarVentas registrarVentas() {
        return new RegistrarVentas(periodos(), determinarCategoria());
    }

    public AnularFactura anularFactura() {
        return new AnularFactura(periodos(), facturas(), determinarCategoria());
    }

    public CerrarPeriodo cerrarPeriodo() {
        return new CerrarPeriodo(periodos(), determinarCategoria());
    }

    Context contexto() {
        return contexto;
    }

    private synchronized Adaptadores adaptadores() {
        if (adaptadores == null) {
            adaptadores = new Adaptadores(contexto, reloj);
        }
        return adaptadores;
    }

    /** Todo lo que depende de la base, creado junto. */
    private static final class Adaptadores {

        final BaseDatosFacturas base;
        final ContribuyenteRepositorioRoom contribuyentes;
        final PeriodoRepositorioRoom periodos;
        final FacturaRepositorioRoom facturas;
        final ParametrosRepositorioRoom parametros;
        final AlmacenImagenes imagenes;
        final GestorPin pin;
        final CargadorParametrosNrus cargador;
        final DeterminarCategoria determinarCategoria;

        Adaptadores(Context contexto, Reloj reloj) {
            base = BaseDatosFacturas.abrir(contexto, GestorClaves.claveBaseDatos(contexto), BaseDatosFacturas.NOMBRE);
            contribuyentes = new ContribuyenteRepositorioRoom(base);
            periodos = new PeriodoRepositorioRoom(base, reloj);
            facturas = new FacturaRepositorioRoom(base, periodos, reloj);
            parametros = new ParametrosRepositorioRoom(base);
            imagenes = new AlmacenImagenes(contexto);
            pin = new GestorPin(base.contribuyenteDao());
            cargador = new CargadorParametrosNrus(contexto, base);
            determinarCategoria = new DeterminarCategoria(periodos, contribuyentes, parametros);
        }
    }
}
