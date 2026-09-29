package pe.facturass20.dominio.puertos;

import pe.facturass20.dominio.modelo.ParametrosNrus;

/**
 * Parámetros del NRUS de la versión activa, cargados desde {@code assets/parametros_nrus.json}
 * (documentación técnica §5.2).
 */
public interface ParametrosRepositorio {

    ParametrosNrus vigentes();
}
