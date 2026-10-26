package pe.facturass20.inferencia;

/**
 * El modelo de lenguaje que corre en el teléfono (documentación técnica §6.1). Lo implementa el puente
 * Kotlin con LiteRT-LM; el resto de la app solo conoce esta interfaz.
 *
 * <p>La imagen va en memoria y nunca se escribe sin cifrar en el teléfono. Una sola instancia en toda la
 * app; sus métodos se llaman desde un hilo de fondo, de a uno.</p>
 */
public interface MotorInferencia extends AutoCloseable {

    /** Carga el modelo; puede tardar unos 10 s. */
    void inicializar(String rutaModelo) throws InferenciaException;

    boolean inicializado();

    /**
     * @param jpeg       la foto ya preparada (§6.4)
     * @param instruccion la instrucción de §6.2
     * @param timeoutMs  si se pasa, se cancela la generación y se lanza la excepción
     * @return el texto que respondió el modelo, sin interpretar
     */
    String generar(byte[] jpeg, String instruccion, long timeoutMs) throws InferenciaException;

    @Override
    void close();
}
