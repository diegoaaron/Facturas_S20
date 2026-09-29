package pe.facturass20.dominio.puertos;

import java.util.Optional;

import pe.facturass20.dominio.modelo.Contribuyente;

/** El único contribuyente del teléfono. El PIN lo maneja aparte la capa de datos. */
public interface ContribuyenteRepositorio {

    /** Vacío mientras no se complete la configuración inicial (P01). */
    Optional<Contribuyente> obtener();

    void guardar(Contribuyente contribuyente);
}
