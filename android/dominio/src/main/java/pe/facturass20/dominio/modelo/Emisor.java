package pe.facturass20.dominio.modelo;

import java.util.Objects;

import pe.facturass20.dominio.reglas.ValidadorRuc;

/** Proveedor que emitió la factura de compra. */
public record Emisor(String ruc, String razonSocial) {

    public Emisor {
        Objects.requireNonNull(ruc, "ruc");
        Objects.requireNonNull(razonSocial, "razonSocial");
        razonSocial = razonSocial.trim();
    }

    public boolean rucValido() {
        return ValidadorRuc.esValido(ruc);
    }
}
