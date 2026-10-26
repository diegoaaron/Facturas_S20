package pe.facturass20.inferencia;

/** El motor no pudo cargar el modelo o generar la respuesta. El mensaje es técnico, para Logcat. */
public class InferenciaException extends Exception {

    /** Si se canceló por pasar el límite de tiempo. */
    private final boolean tiempoAgotado;

    public InferenciaException(String mensaje, Throwable causa) {
        this(mensaje, causa, false);
    }

    public InferenciaException(String mensaje, Throwable causa, boolean tiempoAgotado) {
        super(mensaje, causa);
        this.tiempoAgotado = tiempoAgotado;
    }

    public boolean tiempoAgotado() {
        return tiempoAgotado;
    }
}
