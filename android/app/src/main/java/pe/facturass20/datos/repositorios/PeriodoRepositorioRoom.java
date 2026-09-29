package pe.facturass20.datos.repositorios;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.dao.DeterminacionConCategoria;
import pe.facturass20.datos.dao.PeriodoDao;
import pe.facturass20.datos.entidades.CategoriaNrusEntity;
import pe.facturass20.datos.entidades.ContribuyenteEntity;
import pe.facturass20.datos.entidades.ParametroVersionEntity;
import pe.facturass20.datos.entidades.PeriodoEntity;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.puertos.PeriodoRepositorio;
import pe.facturass20.dominio.puertos.Reloj;

/** Meses del contribuyente, con sus facturas y su determinación. */
public final class PeriodoRepositorioRoom implements PeriodoRepositorio {

    private final BaseDatosFacturas db;
    private final PeriodoDao dao;
    private final Reloj reloj;

    public PeriodoRepositorioRoom(BaseDatosFacturas db, Reloj reloj) {
        this.db = db;
        this.dao = db.periodoDao();
        this.reloj = reloj;
    }

    @Override
    public Optional<PeriodoMensual> buscar(YearMonth periodo) {
        return fila(periodo).map(this::aDominio);
    }

    @Override
    public List<PeriodoMensual> listarDelAnio(int anio) {
        Optional<Long> contribuyente = idContribuyente();
        return contribuyente.isPresent() ? aDominio(dao.listarDelAnio(contribuyente.get(), anio)) : new ArrayList<>();
    }

    @Override
    public List<PeriodoMensual> listar() {
        Optional<Long> contribuyente = idContribuyente();
        return contribuyente.isPresent() ? aDominio(dao.listar(contribuyente.get())) : new ArrayList<>();
    }

    @Override
    public Optional<Determinacion> buscarDeterminacion(YearMonth periodo) {
        return fila(periodo).flatMap(fila -> {
            DeterminacionConCategoria determinacion = dao.buscarDeterminacion(fila.idPeriodo);
            return determinacion == null ? Optional.empty() : Optional.of(Mapeos.aDominio(determinacion, periodo));
        });
    }

    @Override
    public void guardar(PeriodoMensual periodo, Determinacion determinacion) {
        db.runInTransaction(() -> guardarDeterminacion(guardarPeriodo(periodo), determinacion));
    }

    /**
     * Crea o actualiza la fila del mes (ventas y estado) y devuelve su id. Debe llamarse dentro de una
     * transacción; las facturas las guarda {@link FacturaRepositorioRoom}.
     */
    long guardarPeriodo(PeriodoMensual periodo) {
        long contribuyente = idContribuyente()
                .orElseThrow(() -> new IllegalStateException("Falta configurar el contribuyente"));
        PeriodoEntity fila = dao.buscar(contribuyente, periodo.anio(), periodo.mes());
        boolean nueva = fila == null;
        if (nueva) {
            fila = new PeriodoEntity();
            fila.idContribuyente = contribuyente;
            fila.anio = periodo.anio();
            fila.mes = periodo.mes();
        }
        fila.totalVentas = periodo.ventasRegistradas() ? periodo.totalVentas() : null;
        fila.estado = periodo.estado();
        if (nueva) {
            return dao.insertar(fila);
        }
        dao.actualizar(fila);
        return fila.idPeriodo;
    }

    /**
     * Reemplaza la determinación del mes. La categoría se enlaza con la de la versión activa de los
     * parámetros, que es con la que se calculó. Debe llamarse dentro de una transacción.
     */
    void guardarDeterminacion(long idPeriodo, Determinacion determinacion) {
        Long idCategoria = null;
        if (determinacion.categoria() != null) {
            ParametroVersionEntity activa = db.parametrosDao().activa();
            CategoriaNrusEntity categoria = activa == null ? null
                    : db.parametrosDao().buscarCategoria(activa.idParametro, determinacion.categoria().codigo());
            if (categoria == null) {
                throw new IllegalStateException("La categoría " + determinacion.categoria().codigo()
                        + " no está en los parámetros activos");
            }
            idCategoria = categoria.idCategoria;
        }
        dao.guardarDeterminacion(Mapeos.aEntidad(determinacion, idPeriodo, idCategoria, reloj.ahora()));
    }

    Optional<Long> idContribuyente() {
        ContribuyenteEntity contribuyente = db.contribuyenteDao().obtener();
        return contribuyente == null ? Optional.empty() : Optional.of(contribuyente.idContribuyente);
    }

    private Optional<PeriodoEntity> fila(YearMonth periodo) {
        return idContribuyente().map(id -> dao.buscar(id, periodo.getYear(), periodo.getMonthValue()));
    }

    private PeriodoMensual aDominio(PeriodoEntity fila) {
        return new PeriodoMensual(YearMonth.of(fila.anio, fila.mes), fila.totalVentas, fila.estado,
                Mapeos.facturas(db.facturaDao().listarPorPeriodo(fila.idPeriodo)));
    }

    private List<PeriodoMensual> aDominio(List<PeriodoEntity> filas) {
        List<PeriodoMensual> periodos = new ArrayList<>(filas.size());
        for (PeriodoEntity fila : filas) {
            periodos.add(aDominio(fila));
        }
        return periodos;
    }
}
