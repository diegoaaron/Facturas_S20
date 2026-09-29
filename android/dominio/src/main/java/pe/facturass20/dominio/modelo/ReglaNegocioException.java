package pe.facturass20.dominio.modelo;

/**
 * Una operación que las reglas del negocio no permiten (anular en un mes cerrado, cerrar sin ventas...).
 * El mensaje está en español y en lenguaje cotidiano, para mostrarlo tal cual al usuario.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
