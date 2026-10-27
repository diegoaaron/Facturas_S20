package pe.facturass20;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.cifrado.AlmacenImagenes;
import pe.facturass20.datos.cifrado.GestorClaves;
import pe.facturass20.datos.cifrado.GestorPin;
import pe.facturass20.datos.parametros.CargadorParametrosNrus;
import pe.facturass20.datos.repositorios.AvisoRepositorioRoom;
import pe.facturass20.datos.repositorios.ContribuyenteRepositorioRoom;
import pe.facturass20.datos.repositorios.DetallesExportacionRoom;
import pe.facturass20.datos.repositorios.FacturaRepositorioRoom;
import pe.facturass20.datos.repositorios.ParametrosRepositorioRoom;
import pe.facturass20.datos.repositorios.PeriodoRepositorioRoom;
import pe.facturass20.datos.repositorios.ReporteRepositorioRoom;
import pe.facturass20.dominio.casosuso.AnularFactura;
import pe.facturass20.dominio.casosuso.CerrarPeriodo;
import pe.facturass20.dominio.casosuso.DeterminarCategoria;
import pe.facturass20.dominio.casosuso.ExportarDatos;
import pe.facturass20.dominio.casosuso.RegistrarFactura;
import pe.facturass20.dominio.casosuso.RegistrarVentas;
import pe.facturass20.dominio.casosuso.RevisarAvisos;
import pe.facturass20.dominio.casosuso.VerificarFactura;
import pe.facturass20.dominio.puertos.ContribuyenteRepositorio;
import pe.facturass20.dominio.puertos.ExportadorArchivos;
import pe.facturass20.dominio.puertos.ExtraccionException;
import pe.facturass20.dominio.puertos.ExtractorFacturas;
import pe.facturass20.dominio.puertos.FacturaRepositorio;
import pe.facturass20.dominio.puertos.ParametrosRepositorio;
import pe.facturass20.dominio.puertos.PeriodoRepositorio;
import pe.facturass20.dominio.puertos.Reloj;
import pe.facturass20.inferencia.ExtractorFalso;
import pe.facturass20.inferencia.ExtractorGemma;
import pe.facturass20.inferencia.GestorModeloLocal;
import pe.facturass20.inferencia.ManifiestoModelo;
import pe.facturass20.inferencia.litert.LiteRtLmPuente;
import pe.facturass20.trabajos.RecordatorioWorker;
import pe.facturass20.ui.acceso.PreferenciasAcceso;
import pe.facturass20.ui.acceso.Sesion;
import pe.facturass20.ui.captura.CapturaEnCurso;
import pe.facturass20.ui.captura.PreferenciasCaptura;
import pe.facturass20.ui.mes.PreferenciasAvisos;
import pe.facturass20.ui.verificacion.TiemposLectura;

/**
 * Inyección de dependencias manual (documentación técnica §3.1): crea una sola vez los adaptadores
 * que implementan los puertos del dominio y los entrega a los ViewModels.
 *
 * <p>La base de datos y sus repositorios se crean la primera vez que se piden (la clave sale del
 * Keystore), siempre desde un hilo de fondo; en ese momento también se cargan los parámetros del NRUS,
 * así ninguna pantalla los lee antes de tiempo. La primera en pedirlos es {@link Sesion#comprobar()}, al
 * abrir la app. El extractor de facturas es Gemma en el teléfono ({@link ExtractorGemma}) cuando hay un
 * modelo instalado; sin modelo no hay lectura con IA y se registra a mano (P19). {@link ExtractorFalso} solo
 * lo usan las pruebas. Falta agregar el exportador de datos (I5).</p>
 */
public final class ContenedorDependencias {

    private final Context contexto;
    private final ExecutorService ejecutor = Executors.newFixedThreadPool(2);
    /** Un solo hilo para el modelo: nunca corren dos lecturas a la vez (cada una ocupa ~2 GB con Gemma). */
    private final ExecutorService ejecutorInferencia = Executors.newSingleThreadExecutor();
    private final Reloj reloj = Reloj.sistema();
    private final PreferenciasAcceso preferenciasAcceso;
    private final Sesion sesion;
    private final PreferenciasCaptura preferenciasCaptura;
    private final CapturaEnCurso capturaEnCurso = new CapturaEnCurso();
    private final PreferenciasAvisos preferenciasAvisos;
    private final TiemposLectura tiemposLectura;
    private final ExtractorFacturas extractorFalso = new ExtractorFalso(reloj::hoy);
    /** Solo en pruebas: null = el modelo instalado, true = {@link ExtractorFalso}, false = sin lector. */
    private volatile Boolean lectorDePrueba;
    private Adaptadores adaptadores;

    ContenedorDependencias(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        preferenciasAcceso = new PreferenciasAcceso(this.contexto);
        sesion = new Sesion(ejecutor, () -> gestorPin().tienePin());
        preferenciasCaptura = new PreferenciasCaptura(this.contexto);
        preferenciasAvisos = new PreferenciasAvisos(this.contexto);
        tiemposLectura = new TiemposLectura(this.contexto);
    }

    /** Hilos de fondo para la base de datos, la inferencia, el PDF y la exportación; nunca el hilo de UI. */
    public ExecutorService ejecutor() {
        return ejecutor;
    }

    /** Hilo de la lectura con IA (P07). */
    public ExecutorService ejecutorInferencia() {
        return ejecutorInferencia;
    }

    /**
     * Lee los 7 campos de la foto (P07), o null si no hay modelo instalado. Se pide desde un hilo de fondo
     * (consulta la base).
     */
    public ExtractorFacturas extractor() {
        Boolean prueba = lectorDePrueba;
        if (prueba != null) {
            return prueba ? extractorFalso : null;
        }
        return gestorModelo().extractor();
    }

    /** Si hay lectura con IA; si no, P05 lleva la foto a P19 (registro manual). Desde un hilo de fondo. */
    public boolean hayLectorIa() {
        return extractor() != null;
    }

    /**
     * Carga el modelo mientras el usuario toma la foto (P05/P06), para que P07 no sume los segundos de la carga.
     * Va en el hilo de inferencia, así la lectura espera a que termine en lugar de cargarlo otra vez.
     */
    public void precargarLector() {
        ejecutorInferencia.execute(() -> {
            if (extractor() instanceof ExtractorGemma gemma) {
                try {
                    gemma.precargar();
                } catch (ExtraccionException e) {
                    // P07 lo vuelve a intentar y, si falla, ofrece registrar a mano.
                    Log.w("FacturasS20", "No se pudo precargar el modelo");
                }
            }
        });
    }

    /**
     * Libera la memoria del modelo (~3,5 GB) cuando la app deja de verse; se vuelve a cargar al tomar la
     * siguiente foto. Va en el hilo de inferencia para no cortar una lectura en curso, y no abre la base si
     * todavía no se abrió.
     */
    public void liberarLector() {
        Adaptadores abiertos;
        synchronized (this) {
            abiertos = adaptadores;
        }
        if (abiertos != null) {
            ejecutorInferencia.execute(abiertos.gestorModelo::liberar);
        }
    }

    /**
     * Solo para las pruebas, haya o no modelo: true lee con {@link ExtractorFalso}, false se comporta como un
     * teléfono sin modelo y null vuelve al modelo instalado.
     */
    public void usarLectorDePrueba(Boolean usar) {
        lectorDePrueba = usar;
    }

    /** La versión del modelo si {@code extractor} es Gemma, o null si es el lector de prueba (solo en pruebas). */
    public static String versionModelo(ExtractorFacturas extractor) {
        return extractor instanceof ExtractorGemma gemma ? gemma.version() : null;
    }

    /**
     * Si la lectura con {@code extractor} será la primera desde que se cargó el modelo (la más lenta). Se
     * pregunta en el hilo de inferencia, justo antes de leer.
     */
    public static boolean primeraLectura(ExtractorFacturas extractor) {
        return extractor instanceof ExtractorGemma gemma && !gemma.calentado();
    }

    /** Lo que suele tardar {@code extractor} en leer una foto en este teléfono, para el avance de P07. */
    public long duracionEstimada(ExtractorFacturas extractor, boolean primera) {
        return extractor instanceof ExtractorGemma ? tiemposLectura.estimado(primera) : ExtractorFalso.DEMORA_MS;
    }

    /** Ajusta la estimación de P07 con lo que tardó una lectura con Gemma que terminó bien. */
    public void registrarDuracion(ExtractorFacturas extractor, boolean primera, long medidoMs) {
        if (extractor instanceof ExtractorGemma) {
            tiemposLectura.registrar(primera, medidoMs);
        }
    }

    /** El modelo de IA del teléfono: versión activa, descarga e instalación (P03). */
    public GestorModeloLocal gestorModelo() {
        return adaptadores().gestorModelo;
    }

    public Reloj reloj() {
        return reloj;
    }

    /** Si la app está configurada y desbloqueada; dura lo que dura el proceso. */
    public Sesion sesion() {
        return sesion;
    }

    public PreferenciasAcceso preferenciasAcceso() {
        return preferenciasAcceso;
    }

    public PreferenciasCaptura preferenciasCaptura() {
        return preferenciasCaptura;
    }

    /** Avisos del NRUS y recordatorios del vencimiento que el usuario activó (P14, P15). */
    public PreferenciasAvisos preferenciasAvisos() {
        return preferenciasAvisos;
    }

    /** La foto que va de la cámara a la lectura y la verificación (P06 → P07 → P08). */
    public CapturaEnCurso capturaEnCurso() {
        return capturaEnCurso;
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

    /** Los PDF del mes ya generados (P16). */
    public ReporteRepositorioRoom reportes() {
        return adaptadores().reportes;
    }

    /** Lo que la exportación (P20) lee de la base y el dominio no tiene. */
    public DetallesExportacionRoom detallesExportacion() {
        return adaptadores().detallesExportacion;
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

    /** Qué notificaciones tocan hoy (RecordatorioWorker). */
    public RevisarAvisos revisarAvisos() {
        return new RevisarAvisos(periodos(), contribuyentes(), determinarCategoria(), adaptadores().avisos, reloj);
    }

    /** Después de un cambio en los montos: revisa en segundo plano si toca avisar del límite. */
    public void revisarAvisosPronto() {
        RecordatorioWorker.revisarAhora(contexto);
    }

    public ExportarDatos exportarDatos(ExportadorArchivos exportador) {
        return new ExportarDatos(periodos(), exportador);
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

        private static final String ETIQUETA = "FacturasS20";

        final BaseDatosFacturas base;
        final ContribuyenteRepositorioRoom contribuyentes;
        final PeriodoRepositorioRoom periodos;
        final FacturaRepositorioRoom facturas;
        final ParametrosRepositorioRoom parametros;
        final ReporteRepositorioRoom reportes;
        final DetallesExportacionRoom detallesExportacion;
        final AvisoRepositorioRoom avisos;
        final AlmacenImagenes imagenes;
        final GestorPin pin;
        final CargadorParametrosNrus cargador;
        final DeterminarCategoria determinarCategoria;
        final GestorModeloLocal gestorModelo;

        Adaptadores(Context contexto, Reloj reloj) {
            base = BaseDatosFacturas.abrir(contexto, GestorClaves.claveBaseDatos(contexto), BaseDatosFacturas.NOMBRE);
            contribuyentes = new ContribuyenteRepositorioRoom(base);
            periodos = new PeriodoRepositorioRoom(base, reloj);
            facturas = new FacturaRepositorioRoom(base, periodos, reloj);
            parametros = new ParametrosRepositorioRoom(base);
            reportes = new ReporteRepositorioRoom(base, periodos);
            detallesExportacion = new DetallesExportacionRoom(base, periodos);
            avisos = new AvisoRepositorioRoom(base, periodos);
            imagenes = new AlmacenImagenes(contexto);
            pin = new GestorPin(base.contribuyenteDao());
            cargador = new CargadorParametrosNrus(contexto, base);
            determinarCategoria = new DeterminarCategoria(periodos, contribuyentes, parametros);
            // El único motor de la app: cada lectura con Gemma ocupa ~2 GB.
            gestorModelo = new GestorModeloLocal(contexto, base, reloj,
                    new LiteRtLmPuente(contexto.getCacheDir().getAbsolutePath()), manifiestoModelo(contexto));
            cargarParametrosNrus();
        }

        private static ManifiestoModelo manifiestoModelo(Context contexto) {
            try (InputStream entrada = contexto.getAssets().open(ManifiestoModelo.ARCHIVO)) {
                return ManifiestoModelo.leer(entrada);
            } catch (IOException e) {
                // Viene dentro del APK: si falta o está mal, es un error al armar la app.
                throw new IllegalStateException("Falta o está mal assets/" + ManifiestoModelo.ARCHIVO, e);
            }
        }

        /** Carga los parámetros del NRUS si la versión de la app trae una nueva. */
        private void cargarParametrosNrus() {
            try {
                if (cargador.cargarSiHaceFalta()) {
                    Log.i(ETIQUETA, "Parámetros del NRUS actualizados");
                }
            } catch (RuntimeException e) {
                // La app abre igual; las pantallas que los necesitan muestran un mensaje.
                Log.e(ETIQUETA, "No se pudieron cargar los parámetros del NRUS", e);
            }
        }
    }
}
