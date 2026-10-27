package pe.facturass20.datos.repositorios;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.entidades.AvisoEntity;
import pe.facturass20.dominio.modelo.Aviso;
import pe.facturass20.dominio.puertos.AvisoRepositorio;

/** Los avisos ya mandados de cada mes (tabla {@code aviso}). */
public final class AvisoRepositorioRoom implements AvisoRepositorio {

    private final BaseDatosFacturas db;
    private final PeriodoRepositorioRoom periodos;

    public AvisoRepositorioRoom(BaseDatosFacturas db, PeriodoRepositorioRoom periodos) {
        this.db = db;
        this.periodos = periodos;
    }

    @Override
    public List<Aviso> listar(YearMonth periodo) {
        List<Aviso> avisos = new ArrayList<>();
        Optional<Long> idPeriodo = periodos.idPeriodo(periodo);
        if (idPeriodo.isPresent()) {
            for (AvisoEntity fila : db.avisoDao().listarPorPeriodo(idPeriodo.get())) {
                avisos.add(new Aviso(periodo, fila.tipo, fila.fechaProgramada, fila.enviado));
            }
        }
        return avisos;
    }

    @Override
    public void registrar(Aviso aviso) {
        AvisoEntity fila = new AvisoEntity();
        fila.idPeriodo = periodos.idPeriodo(aviso.periodo())
                .orElseThrow(() -> new IllegalStateException("El mes del aviso no existe"));
        fila.tipo = aviso.tipo();
        fila.fechaProgramada = aviso.fechaProgramada();
        fila.enviado = aviso.enviado();
        db.avisoDao().insertar(fila);
    }
}
