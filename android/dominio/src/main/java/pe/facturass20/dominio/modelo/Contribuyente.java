package pe.facturass20.dominio.modelo;

import java.time.LocalDate;
import java.util.Objects;

import pe.facturass20.dominio.reglas.ValidadorRuc;

/**
 * El titular del negocio que usa la app (hay uno solo por teléfono). El PIN no está aquí: su hash y su
 * sal los guarda la capa de datos.
 */
public record Contribuyente(String ruc, String nombre, String titular, LocalDate fechaAlta) {

    public Contribuyente {
        if (!ValidadorRuc.esValido(ruc)) {
            throw new ReglaNegocioException("El RUC no es válido. Revise los 11 dígitos.");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ReglaNegocioException("Escriba el nombre del negocio.");
        }
        if (titular == null || titular.trim().isEmpty()) {
            throw new ReglaNegocioException("Escriba el nombre del titular.");
        }
        nombre = nombre.trim();
        titular = titular.trim();
        Objects.requireNonNull(fechaAlta, "fechaAlta");
    }

    /** Último dígito del RUC: decide la fecha de vencimiento en el cronograma de la SUNAT. */
    public int ultimoDigitoRuc() {
        return ruc.charAt(10) - '0';
    }
}
