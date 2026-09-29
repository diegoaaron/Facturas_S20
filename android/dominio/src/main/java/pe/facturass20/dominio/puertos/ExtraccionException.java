package pe.facturass20.dominio.puertos;

/**
 * No se pudo leer la factura (el modelo no respondió un JSON válido, se agotó el tiempo...). El mensaje
 * es para el usuario, p. ej. «No pudimos leer la factura. Tome otra foto con más luz.»
 */
public final class ExtraccionException extends Exception {

    public ExtraccionException(String mensaje) {
        super(mensaje);
    }

    public ExtraccionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
