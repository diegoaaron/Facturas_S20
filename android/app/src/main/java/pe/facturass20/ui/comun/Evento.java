package pe.facturass20.ui.comun;

/**
 * Valor de un {@code LiveData} que se atiende una sola vez (un mensaje, una navegación). Así, al girar
 * la pantalla, el Fragment nuevo no vuelve a mostrar el mismo mensaje.
 */
public final class Evento<T> {

    private final T contenido;
    private boolean atendido;

    public Evento(T contenido) {
        this.contenido = contenido;
    }

    /** El contenido la primera vez; {@code null} las siguientes. */
    public T tomar() {
        if (atendido) {
            return null;
        }
        atendido = true;
        return contenido;
    }
}
