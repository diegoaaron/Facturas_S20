package pe.facturass20.dominio.modelo;

import java.util.Objects;

/** Versión del modelo Gemma instalada (o por instalar) en el teléfono (documentación técnica §6.6). */
public record ModeloLocal(String version, int tamanoMb, String sha256, EstadoModelo estado, String ruta) {

    public ModeloLocal {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(sha256, "sha256");
        Objects.requireNonNull(estado, "estado");
    }

    /** Compara la huella calculada del archivo descargado con la del manifiesto {@code assets/modelo.json}. */
    public boolean verificarIntegridad(String sha256Calculado) {
        return sha256.equalsIgnoreCase(sha256Calculado);
    }
}
