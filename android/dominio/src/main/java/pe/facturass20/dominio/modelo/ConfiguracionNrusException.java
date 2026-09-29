package pe.facturass20.dominio.modelo;

/**
 * Faltan datos en los parámetros del NRUS ({@code assets/parametros_nrus.json}): no hay categorías
 * vigentes o el cronograma no trae la fecha de vencimiento. Nunca se inventa el dato que falta.
 */
public final class ConfiguracionNrusException extends IllegalStateException {

    public ConfiguracionNrusException(String mensaje) {
        super(mensaje);
    }
}
