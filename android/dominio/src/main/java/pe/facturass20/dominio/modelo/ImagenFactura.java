package pe.facturass20.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Foto de la factura, guardada cifrada en el teléfono.
 *
 * @param ruta    archivo cifrado en {@code filesDir/imagenes/}
 * @param nitidez varianza del Laplaciano de la imagen en gris (documentación técnica §6.4)
 */
public record ImagenFactura(String ruta, double nitidez, LocalDateTime fechaCaptura) {

    public ImagenFactura {
        Objects.requireNonNull(ruta, "ruta");
        Objects.requireNonNull(fechaCaptura, "fechaCaptura");
    }

    public boolean esLegible(double umbralNitidez) {
        return nitidez >= umbralNitidez;
    }
}
