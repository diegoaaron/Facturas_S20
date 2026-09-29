package pe.facturass20.ui.captura;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Foto ya preparada para la lectura (§6.4: recortada al marco, derecha, lado mayor de 896 px, JPEG 90).
 * Solo vive en memoria hasta que P08 la guarda cifrada con {@code AlmacenImagenes}; nunca se escribe sin
 * cifrar en el teléfono.
 *
 * @param evaluacion nitidez y luz de la foto final (la nitidez va a {@code imagen_factura.nitidez})
 */
public record FotoCapturada(byte[] jpeg, EvaluadorNitidez.Evaluacion evaluacion, LocalDateTime fechaCaptura,
                            boolean deGaleria) {

    public FotoCapturada {
        Objects.requireNonNull(jpeg, "jpeg");
        Objects.requireNonNull(evaluacion, "evaluacion");
        Objects.requireNonNull(fechaCaptura, "fechaCaptura");
    }
}
