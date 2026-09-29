package pe.facturass20.datos.repositorios;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.dao.ParametrosDao;
import pe.facturass20.datos.entidades.ParametroVersionEntity;
import pe.facturass20.dominio.modelo.ConfiguracionNrusException;
import pe.facturass20.dominio.modelo.ParametrosNrus;
import pe.facturass20.dominio.puertos.ParametrosRepositorio;

/** La versión activa de los parámetros, que carga {@code CargadorParametrosNrus} al iniciar la app. */
public final class ParametrosRepositorioRoom implements ParametrosRepositorio {

    private final ParametrosDao dao;

    public ParametrosRepositorioRoom(BaseDatosFacturas db) {
        this.dao = db.parametrosDao();
    }

    @Override
    public ParametrosNrus vigentes() {
        ParametroVersionEntity activa = dao.activa();
        if (activa == null) {
            throw new ConfiguracionNrusException("Todavía no se cargaron los parámetros del NRUS");
        }
        return Mapeos.aDominio(activa, dao.categorias(activa.idParametro), dao.cronograma(activa.idParametro));
    }
}
