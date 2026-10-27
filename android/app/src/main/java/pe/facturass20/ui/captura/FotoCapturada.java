package pe.facturass20.ui.captura;

import java.time.LocalDateTime;
import java.util.Objects;

import pe.facturass20.dominio.modelo.DatosQr;

/**
 * Foto ya preparada para la lectura (§6.4: recortada al marco, derecha, lado mayor de 896 px, JPEG 90).
 * Solo vive en memoria hasta que P08 la guarda cifrada con {@code AlmacenImagenes}; nunca se escribe sin
 * cifrar en el teléfono.
 *
 * @param evaluacion nitidez y luz de la foto final (la nitidez va a {@code imagen_factura.nitidez})
 * @param qr         lo que trae el QR del comprobante, leído antes de reducir la foto; nulo si no se encontró
 */
public record FotoCapturada(byte[] jpeg, EvaluadorNitidez.Evaluacion evaluacion, LocalDateTime fechaCaptura,
                            boolean deGaleria, DatosQr qr) {

    public FotoCapturada {
        Objects.requireNonNull(jpeg, "jpeg");
        Objects.requireNonNull(evaluacion, "evaluacion");
        Objects.requireNonNull(fechaCaptura, "fechaCaptura");
    }
}
