package pe.facturass20.dominio.modelo;

/**
 * Avisos que manda la app: 80 % y 100 % del límite de la categoría (o fuera del régimen), acercarse al tope
 * anual y vencimiento de la declaración. Se guardan por nombre en la tabla {@code aviso}.
 */
public enum TipoAviso {
    TOPE_80, TOPE_100, TOPE_ANUAL, VENCIMIENTO
}
