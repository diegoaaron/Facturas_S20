package pe.facturass20.dominio.modelo;

/**
 * Qué quiere exportar el usuario en P20 (documentación técnica §9).
 *
 * @param mes 1 a 12, o nulo para exportar el año completo
 */
public record SolicitudExportacion(int anio, Integer mes, boolean incluirImagenes, boolean incluirReporte) {

    public SolicitudExportacion {
        if (mes != null && (mes < 1 || mes > 12)) {
            throw new IllegalArgumentException("Mes fuera de rango: " + mes);
        }
    }

    public boolean esAnual() {
        return mes == null;
    }
}
