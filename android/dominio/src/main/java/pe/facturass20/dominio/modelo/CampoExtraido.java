package pe.facturass20.dominio.modelo;

import java.util.Objects;

/**
 * Un campo leído por el modelo y el valor con el que se quedó el usuario en P08.
 *
 * @param valorLeido lo que devolvió el modelo; nulo si no lo pudo leer
 * @param valorFinal lo que se guarda, después de la revisión del usuario
 * @param confianza  entre 0 y 1; 0 si el campo no se leyó
 */
public record CampoExtraido(CampoFactura nombre, String valorLeido, String valorFinal, double confianza) {

    public CampoExtraido {
        Objects.requireNonNull(nombre, "nombre");
        if (confianza < 0 || confianza > 1 || Double.isNaN(confianza)) {
            throw new IllegalArgumentException("Confianza fuera de [0, 1]: " + confianza);
        }
    }

    /** Campo tal como lo leyó el modelo, todavía sin revisar. */
    public static CampoExtraido leido(CampoFactura nombre, String valor, double confianza) {
        return new CampoExtraido(nombre, valor, valor, confianza);
    }

    public CampoExtraido conValorFinal(String valor) {
        return new CampoExtraido(nombre, valorLeido, valor, confianza);
    }

    /** El usuario cambió lo que leyó el modelo. Sirve para medir la exactitud real del modelo. */
    public boolean corregido() {
        return !Objects.equals(valorLeido, valorFinal);
    }
}
