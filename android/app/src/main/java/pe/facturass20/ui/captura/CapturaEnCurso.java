package pe.facturass20.ui.captura;

import pe.facturass20.dominio.modelo.ResultadoExtraccion;

/**
 * La foto que P05/P06 le pasan a P07 Lectura y P08 Verificación, y lo que leyó la IA (P07 → P08). Va en el
 * contenedor de dependencias y no en los argumentos de navegación porque pesa demasiado para un
 * {@code Bundle} y no debe escribirse sin cifrar. Si el sistema cierra el proceso se pierde, igual que la
 * sesión: hay que tomar otra foto.
 *
 * <p>Se usa desde el hilo de UI.</p>
 */
public final class CapturaEnCurso {

    private FotoCapturada foto;
    private ResultadoExtraccion extraccion;

    /** Una foto nueva descarta la lectura de la anterior. */
    public void poner(FotoCapturada foto) {
        this.foto = foto;
        this.extraccion = null;
    }

    /** La última foto, o {@code null} si no hay. */
    public FotoCapturada foto() {
        return foto;
    }

    public void ponerExtraccion(ResultadoExtraccion extraccion) {
        this.extraccion = extraccion;
    }

    /** Lo que leyó la IA de {@link #foto()}, o {@code null} si todavía no se lee. */
    public ResultadoExtraccion extraccion() {
        return extraccion;
    }

    /** Al guardar o descartar la factura. */
    public void limpiar() {
        foto = null;
        extraccion = null;
    }
}
