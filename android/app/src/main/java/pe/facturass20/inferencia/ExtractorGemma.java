package pe.facturass20.inferencia;

import android.os.SystemClock;
import android.util.Log;

import pe.facturass20.dominio.modelo.ResultadoExtraccion;
import pe.facturass20.dominio.puertos.ExtraccionException;
import pe.facturass20.dominio.puertos.ExtractorFacturas;

/**
 * Lee los 7 campos de la foto con Gemma 4 en el teléfono (RF-04): manda la foto y la instrucción al
 * {@link MotorInferencia} y arma el resultado con {@link ParserRespuesta}. La foto se achica a 768 px solo
 * para el modelo ({@link ReductorFoto}).
 *
 * <p>El modelo se carga la primera vez que se usa, o antes con {@link #precargar()} para que la primera
 * lectura no sume los segundos de la carga. Se usa desde un solo hilo (el de inferencia).</p>
 */
public final class ExtractorGemma implements ExtractorFacturas {

    private static final String ETIQUETA = "FacturasS20";
    /**
     * Límite de la generación. RNF-04 pide 20 s, pero el modelo base tarda 47–68 s en el Moto G34
     * (§6.5): se deja margen para no cortar lecturas que sí terminan, y la RNF se reporta como no cumplida.
     */
    public static final long LIMITE_MS = 90_000;
    /** Lo que suele tardar una lectura, para el avance de P07. */
    public static final long ESTIMADO_MS = 55_000;

    private final MotorInferencia motor;
    private final String rutaModelo;
    private final String version;

    /**
     * @param rutaModelo el archivo {@code .litertlm} ya verificado
     * @param version    va en {@code campo_extraido}, para saber qué modelo leyó cada factura
     */
    public ExtractorGemma(MotorInferencia motor, String rutaModelo, String version) {
        this.motor = motor;
        this.rutaModelo = rutaModelo;
        this.version = version;
    }

    public String version() {
        return version;
    }

    /** Carga el modelo si todavía no está cargado; tarda unos segundos. */
    public void precargar() throws ExtraccionException {
        if (motor.inicializado()) {
            return;
        }
        long inicio = SystemClock.elapsedRealtime();
        try {
            motor.inicializar(rutaModelo);
        } catch (InferenciaException e) {
            Log.e(ETIQUETA, "No se pudo cargar el modelo " + version, e);
            throw new ExtraccionException("No se pudo preparar el lector de facturas. Registre la factura a mano.", e);
        }
        Log.i(ETIQUETA, "Modelo " + version + " cargado en " + (SystemClock.elapsedRealtime() - inicio) + " ms");
    }

    @Override
    public ResultadoExtraccion extraer(byte[] imagenJpeg) throws ExtraccionException {
        precargar();
        long inicio = SystemClock.elapsedRealtime();
        String respuesta;
        try {
            respuesta = motor.generar(ReductorFoto.reducir(imagenJpeg, ReductorFoto.LADO_MODELO),
                    ParserRespuesta.INSTRUCCION, LIMITE_MS);
        } catch (InferenciaException e) {
            Log.e(ETIQUETA, "Falló la lectura con el modelo " + version, e);
            throw new ExtraccionException(e.tiempoAgotado()
                    ? "La lectura tardó demasiado. Tome otra foto o registre la factura a mano."
                    : ParserRespuesta.SIN_JSON, e);
        }
        long ms = SystemClock.elapsedRealtime() - inicio;
        Log.i(ETIQUETA, "Lectura en " + ms + " ms");
        return new ResultadoExtraccion(version, ms, ParserRespuesta.interpretar(respuesta));
    }

    /** Libera la memoria del modelo (onTrimMemory crítico); se vuelve a cargar al siguiente uso. */
    public void liberar() {
        motor.close();
    }
}
