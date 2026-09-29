package pe.facturass20.datos.repositorios;

import java.util.Optional;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.dao.ContribuyenteDao;
import pe.facturass20.datos.entidades.ContribuyenteEntity;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.puertos.ContribuyenteRepositorio;

/** El contribuyente en la tabla {@code contribuyente}. El PIN lo maneja {@code GestorPin}. */
public final class ContribuyenteRepositorioRoom implements ContribuyenteRepositorio {

    private final ContribuyenteDao dao;

    public ContribuyenteRepositorioRoom(BaseDatosFacturas db) {
        this.dao = db.contribuyenteDao();
    }

    @Override
    public Optional<Contribuyente> obtener() {
        ContribuyenteEntity fila = dao.obtener();
        return fila == null ? Optional.empty() : Optional.of(Mapeos.aDominio(fila));
    }

    /** Crea la fila la primera vez; después la actualiza, conservando su id y su PIN. */
    @Override
    public void guardar(Contribuyente contribuyente) {
        ContribuyenteEntity fila = dao.obtener();
        if (fila == null) {
            fila = new ContribuyenteEntity();
            Mapeos.copiar(contribuyente, fila);
            dao.insertar(fila);
        } else {
            Mapeos.copiar(contribuyente, fila);
            dao.actualizar(fila);
        }
    }
}
