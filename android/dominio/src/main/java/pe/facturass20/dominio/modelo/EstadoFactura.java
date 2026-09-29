package pe.facturass20.dominio.modelo;

/** Una factura anulada no se borra: queda con su motivo y deja de sumar al acumulado. */
public enum EstadoFactura {
    VIGENTE, ANULADA
}
