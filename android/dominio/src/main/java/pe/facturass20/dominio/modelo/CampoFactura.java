package pe.facturass20.dominio.modelo;

/**
 * Los 7 campos que se leen de una factura. La {@link #clave()} es el nombre del campo en el JSON que
 * responde el modelo (documentación técnica §6.2); si cambia, se actualiza también el cuaderno de
 * entrenamiento, la instrucción, el parser y la tabla {@code campo_extraido}.
 */
public enum CampoFactura {
    RUC("ruc", true),
    RAZON_SOCIAL("razon_social", false),
    SERIE("serie", true),
    NUMERO("numero", true),
    FECHA_EMISION("fecha_emision", true),
    MONEDA("moneda", false),
    IMPORTE_TOTAL("importe_total", true);

    private final String clave;
    private final boolean esClave;

    CampoFactura(String clave, boolean esClave) {
        this.clave = clave;
        this.esClave = esClave;
    }

    /** Nombre del campo en el JSON del modelo, p. ej. {@code "fecha_emision"}. */
    public String clave() {
        return clave;
    }

    /** Campos que cuentan para la confianza global: RUC, serie, número, fecha e importe (§6.3). */
    public boolean esClave() {
        return esClave;
    }
}
