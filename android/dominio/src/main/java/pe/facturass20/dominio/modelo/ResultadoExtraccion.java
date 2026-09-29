package pe.facturass20.dominio.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import pe.facturass20.dominio.reglas.ValidadorCampos;

/**
 * Lo que devolvió el modelo para una foto: los 7 campos con su confianza (documentación técnica §6.3).
 * Es inmutable; las correcciones del usuario crean un resultado nuevo con {@link #conValorFinal}.
 */
public record ResultadoExtraccion(String versionModelo, long tiempoMs, List<CampoExtraido> campos) {

    /** Por debajo de esta confianza el campo se muestra en ámbar para que el usuario lo revise (RF-05). */
    public static final double UMBRAL_CONFIANZA = 0.80;
    /** Si la confianza global queda por debajo, se sugiere tomar otra foto. */
    public static final double UMBRAL_OTRA_FOTO = 0.50;

    public ResultadoExtraccion {
        Objects.requireNonNull(versionModelo, "versionModelo");
        campos = List.copyOf(campos);
    }

    public Optional<CampoExtraido> campo(CampoFactura nombre) {
        return campos.stream().filter(c -> c.nombre() == nombre).findFirst();
    }

    /** Campos con confianza menor a 0,80, que faltan o cuyo valor final no pasa la validación. */
    public List<CampoFactura> camposARevisar() {
        List<CampoFactura> revisar = new ArrayList<>();
        for (CampoFactura nombre : CampoFactura.values()) {
            Optional<CampoExtraido> campo = campo(nombre);
            if (campo.isEmpty()
                    || campo.get().confianza() < UMBRAL_CONFIANZA
                    || ValidadorCampos.error(nombre, campo.get().valorFinal()).isPresent()) {
                revisar.add(nombre);
            }
        }
        return revisar;
    }

    /** La menor confianza entre los campos clave (RUC, serie, número, fecha e importe); 0 si falta alguno. */
    public double confianzaGlobal() {
        double minima = 1;
        for (CampoFactura nombre : CampoFactura.values()) {
            if (nombre.esClave()) {
                minima = Math.min(minima, campo(nombre).map(CampoExtraido::confianza).orElse(0.0));
            }
        }
        return minima;
    }

    public boolean sugiereOtraFoto() {
        return confianzaGlobal() < UMBRAL_OTRA_FOTO;
    }

    /** Copia con el valor final de un campo cambiado por el usuario. */
    public ResultadoExtraccion conValorFinal(CampoFactura nombre, String valor) {
        List<CampoExtraido> nuevos = new ArrayList<>();
        boolean encontrado = false;
        for (CampoExtraido campo : campos) {
            if (campo.nombre() == nombre) {
                nuevos.add(campo.conValorFinal(valor));
                encontrado = true;
            } else {
                nuevos.add(campo);
            }
        }
        if (!encontrado) {
            nuevos.add(new CampoExtraido(nombre, null, valor, 0));
        }
        return new ResultadoExtraccion(versionModelo, tiempoMs, nuevos);
    }
}
