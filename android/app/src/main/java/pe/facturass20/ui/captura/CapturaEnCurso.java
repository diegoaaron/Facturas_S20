package pe.facturass20.ui.captura;

/**
 * La foto que P05/P06 le pasan a P07 Lectura y P08 Verificación. Va en el contenedor de dependencias y no
 * en los argumentos de navegación porque pesa demasiado para un {@code Bundle} y no debe escribirse sin
 * cifrar. Si el sistema cierra el proceso se pierde, igual que la sesión: hay que tomar otra foto.
 *
 * <p>Se usa desde el hilo de UI.</p>
 */
public final class CapturaEnCurso {

    private FotoCapturada foto;

    public void poner(FotoCapturada foto) {
        this.foto = foto;
    }

    /** La última foto, o {@code null} si no hay. */
    public FotoCapturada foto() {
        return foto;
    }

    /** Al guardar o descartar la factura. */
    public void limpiar() {
        foto = null;
    }
}
