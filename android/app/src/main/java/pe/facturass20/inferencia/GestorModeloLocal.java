package pe.facturass20.inferencia;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.StatFs;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.function.LongConsumer;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.entidades.ModeloLocalEntity;
import pe.facturass20.dominio.modelo.EstadoModelo;
import pe.facturass20.dominio.puertos.ExtraccionException;
import pe.facturass20.dominio.puertos.Reloj;

/**
 * El modelo de IA del teléfono (documentación técnica §6.6): qué versión está activa, si hay una nueva en
 * el manifiesto y cómo se instala un archivo descargado (tamaño, SHA-256, prueba con la factura de control,
 * activación en {@code modelo_local} y borrado de la versión anterior).
 *
 * <p>Una sola instancia, en {@code ContenedorDependencias}, con el único {@link MotorInferencia} de la app.
 * Sus métodos leen la base o archivos grandes: se llaman desde un hilo de fondo.</p>
 */
public final class GestorModeloLocal {

    public enum Estado {
        /** Procesador de 32 bits (sin LiteRT-LM) o poca memoria para Gemma: solo registro manual. */
        INCOMPATIBLE,
        /** No hay modelo instalado: se ofrece la descarga en P03. */
        SIN_MODELO,
        /** Hay un modelo activo, pero el manifiesto trae una versión nueva. */
        ACTUALIZACION,
        LISTO
    }

    /** Los teléfonos de «6 GB» reportan algo menos de memoria total (§6.6). */
    public static final long MEMORIA_MINIMA = 5_500L * 1024 * 1024;
    /** El modelo afinado (4,9 GiB) más las cachés que LiteRT-LM crea al cargarlo (~2,3 GB) (§6.6). */
    public static final long ESPACIO_MINIMO = 8_000L * 1024 * 1024;

    private static final String ETIQUETA = "FacturasS20";
    static final String CARPETA = "modelos";
    static final String FACTURA_CONTROL = "factura_control.jpg";
    /** La primera lectura después de cargar el modelo es la más lenta: más margen que en P07. */
    private static final long LIMITE_PRUEBA_MS = 120_000;

    private final Context contexto;
    private final BaseDatosFacturas base;
    private final Reloj reloj;
    private final MotorInferencia motor;
    private final ManifiestoModelo manifiesto;
    private ExtractorGemma extractor;

    public GestorModeloLocal(Context contexto, BaseDatosFacturas base, Reloj reloj, MotorInferencia motor,
            ManifiestoModelo manifiesto) {
        this.contexto = contexto.getApplicationContext();
        this.base = base;
        this.reloj = reloj;
        this.motor = motor;
        this.manifiesto = manifiesto;
    }

    public ManifiestoModelo manifiesto() {
        return manifiesto;
    }

    /** Si el teléfono puede correr Gemma: procesador arm64 y memoria suficiente (§6.6, paso 1). */
    public boolean telefonoCompatible() {
        return compatible(Arrays.asList(Build.SUPPORTED_64_BIT_ABIS).contains("arm64-v8a"), memoriaTotal(contexto));
    }

    static boolean compatible(boolean arm64, long memoriaTotal) {
        return arm64 && memoriaTotal >= MEMORIA_MINIMA;
    }

    public static long memoriaTotal(Context contexto) {
        ActivityManager.MemoryInfo memoria = new ActivityManager.MemoryInfo();
        ((ActivityManager) contexto.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(memoria);
        return memoria.totalMem;
    }

    /** Espacio libre donde se guarda el modelo. */
    public long espacioLibre() {
        return new StatFs(contexto.getFilesDir().getAbsolutePath()).getAvailableBytes();
    }

    /**
     * Si cabe el modelo para descargarlo: lo ya descargado en el archivo parcial cuenta como espacio, porque
     * al reanudar no se vuelve a pedir.
     */
    static boolean espacioSuficiente(long libre, long yaDescargado) {
        return libre + yaDescargado >= ESPACIO_MINIMO;
    }

    public boolean hayEspacioParaDescargar() {
        return espacioSuficiente(espacioLibre(), archivoParcial().length());
    }

    public synchronized Estado estado() {
        if (!telefonoCompatible()) {
            return Estado.INCOMPATIBLE;
        }
        ModeloLocalEntity activo = activo();
        if (activo == null) {
            return Estado.SIN_MODELO;
        }
        return activo.version.equals(manifiesto.version()) ? Estado.LISTO : Estado.ACTUALIZACION;
    }

    /** La fila del modelo activo, si su archivo sigue en el teléfono. */
    public synchronized ModeloLocalEntity activo() {
        ModeloLocalEntity activo = base.modeloDao().activo();
        return activo != null && activo.ruta != null && new File(activo.ruta).isFile() ? activo : null;
    }

    /**
     * El lector con el modelo activo, o nulo si no hay modelo o el teléfono no puede correrlo (se registra a
     * mano).
     */
    public synchronized ExtractorGemma extractor() {
        ModeloLocalEntity activo = telefonoCompatible() ? activo() : null;
        if (activo == null) {
            extractor = null;
        } else if (extractor == null || !extractor.version().equals(activo.version)) {
            extractor = new ExtractorGemma(motor, activo.ruta, activo.version);
        }
        return extractor;
    }

    /** Libera la memoria del modelo; se vuelve a cargar en la siguiente lectura. */
    public synchronized void liberar() {
        motor.close();
    }

    /** Donde la descarga escribe el modelo del manifiesto mientras baja. */
    public File archivoParcial() {
        return new File(carpeta(), manifiesto.nombreArchivo() + ".part");
    }

    /**
     * Una copia del modelo puesta con adb en la carpeta externa de la app
     * ({@code Android/data/pe.facturass20/files/modelos/modelo.litertlm}), para no descargar 2,6 GB en cada
     * teléfono de prueba. Nulo si no hay o no tiene el tamaño del manifiesto.
     */
    public File copiaExterna() {
        File carpeta = contexto.getExternalFilesDir(CARPETA);
        File copia = carpeta == null ? null : new File(carpeta, "modelo.litertlm");
        return copia != null && copia.length() == manifiesto.tamanoBytes() ? copia : null;
    }

    /** Copia {@code origen} al archivo parcial, para instalarlo después con {@link #instalar}. */
    public void copiarAParcial(File origen, LongConsumer avance) throws IOException {
        File parcial = archivoParcial();
        byte[] bloque = new byte[1 << 20];
        long copiados = 0;
        try (InputStream entrada = new FileInputStream(origen); OutputStream salida = new FileOutputStream(parcial)) {
            for (int n; (n = entrada.read(bloque)) > 0; ) {
                salida.write(bloque, 0, n);
                copiados += n;
                avance.accept(copiados);
            }
        } catch (IOException e) {
            borrar(parcial);
            throw e;
        }
    }

    /**
     * Pasos 4 a 6 de §6.6 con el archivo parcial ya completo: verifica tamaño y SHA-256, prueba el modelo
     * con la factura de control, lo activa y borra la versión anterior. Si algo falla, borra el archivo
     * nuevo y sigue la versión anterior.
     *
     * @param avanceVerificacion bytes verificados hasta el momento
     */
    public void instalar(LongConsumer avanceVerificacion) throws IOException {
        File parcial = archivoParcial();
        if (parcial.length() != manifiesto.tamanoBytes()) {
            borrar(parcial);
            throw new IOException("El modelo descargado está incompleto");
        }
        String huella = Huella.sha256(parcial, avanceVerificacion);
        if (!huella.equals(manifiesto.sha256())) {
            borrar(parcial);
            throw new IOException("El modelo descargado no coincide con su huella SHA-256");
        }
        File destino = new File(carpeta(), manifiesto.nombreArchivo());
        if (!parcial.renameTo(destino)) {
            borrar(parcial);
            throw new IOException("No se pudo guardar el modelo");
        }
        synchronized (this) {
            try {
                probar(destino);
            } catch (IOException e) {
                motor.close();
                borrar(destino);
                throw e;
            }
            activar(destino);
        }
    }

    /** Paso 5: el modelo carga y responde un JSON para la factura de control. Queda cargado en el motor. */
    private void probar(File modelo) throws IOException {
        byte[] control;
        try (InputStream entrada = contexto.getAssets().open(FACTURA_CONTROL);
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            // Sin readAllBytes(): no existe antes de Android 13.
            byte[] bloque = new byte[8192];
            for (int n; (n = entrada.read(bloque)) > 0; ) {
                salida.write(bloque, 0, n);
            }
            control = ReductorFoto.reducir(salida.toByteArray(), ReductorFoto.LADO_MODELO);
        }
        try {
            motor.close();
            motor.inicializar(modelo.getAbsolutePath());
            ParserRespuesta.interpretar(motor.generar(control, ParserRespuesta.INSTRUCCION, LIMITE_PRUEBA_MS));
        } catch (InferenciaException | ExtraccionException e) {
            throw new IOException("El modelo no pasó la prueba con la factura de control", e);
        }
    }

    /** Paso 6: el nuevo queda ACTIVO y el anterior se borra (RNF-19). */
    private void activar(File modelo) {
        ModeloLocalEntity anterior = base.modeloDao().activo();
        // Se toma antes de la transacción, que la deja en null.
        String rutaAnterior = anterior != null ? anterior.ruta : null;
        base.runInTransaction(() -> {
            if (anterior != null) {
                anterior.estado = EstadoModelo.NO_INSTALADO;
                anterior.ruta = null;
                base.modeloDao().actualizar(anterior);
            }
            ModeloLocalEntity nuevo = base.modeloDao().buscarPorVersion(manifiesto.version());
            boolean existe = nuevo != null;
            if (!existe) {
                nuevo = new ModeloLocalEntity();
                nuevo.version = manifiesto.version();
            }
            nuevo.tamanoMb = (int) (manifiesto.tamanoBytes() / (1024 * 1024));
            nuevo.sha256 = manifiesto.sha256();
            nuevo.estado = EstadoModelo.ACTIVO;
            nuevo.ruta = modelo.getAbsolutePath();
            nuevo.fechaInstalacion = reloj.ahora();
            if (existe) {
                base.modeloDao().actualizar(nuevo);
            } else {
                base.modeloDao().insertar(nuevo);
            }
        });
        if (rutaAnterior != null && !rutaAnterior.equals(modelo.getAbsolutePath())) {
            borrar(new File(rutaAnterior));
        }
        extractor = new ExtractorGemma(motor, modelo.getAbsolutePath(), manifiesto.version());
        Log.i(ETIQUETA, "Modelo " + manifiesto.version() + " activo");
    }

    private File carpeta() {
        File carpeta = new File(contexto.getFilesDir(), CARPETA);
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
            Log.w(ETIQUETA, "No se pudo crear la carpeta de modelos");
        }
        return carpeta;
    }

    private static void borrar(File archivo) {
        if (archivo.exists() && !archivo.delete()) {
            Log.w(ETIQUETA, "No se pudo borrar " + archivo.getName());
        }
    }
}
