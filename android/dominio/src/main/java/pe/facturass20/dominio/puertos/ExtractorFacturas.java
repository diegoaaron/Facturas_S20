package pe.facturass20.dominio.puertos;

import pe.facturass20.dominio.modelo.ResultadoExtraccion;

/**
 * Lee los 7 campos de la foto de una factura (documentación técnica §6.1). Lo implementa
 * {@code ExtractorGemmaLocal} con el modelo en el teléfono; mientras tanto, uno falso.
 */
public interface ExtractorFacturas {

    /**
     * Se llama en un hilo de fondo: puede tardar hasta 20 s.
     *
     * @param imagenJpeg foto ya recortada y reducida a 896 px por el lado mayor
     */
    ResultadoExtraccion extraer(byte[] imagenJpeg) throws ExtraccionException;
}
