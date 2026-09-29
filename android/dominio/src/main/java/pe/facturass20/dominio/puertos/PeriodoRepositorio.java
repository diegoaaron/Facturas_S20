package pe.facturass20.dominio.puertos;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.PeriodoMensual;

/** Meses del contribuyente con sus facturas y su determinación. */
public interface PeriodoRepositorio {

    /** El mes con todas sus facturas (vigentes y anuladas), si existe. */
    Optional<PeriodoMensual> buscar(YearMonth periodo);

    /** Los meses de ese año, con sus facturas, del más antiguo al más reciente. */
    List<PeriodoMensual> listarDelAnio(int anio);

    /** Todos los meses, del más reciente al más antiguo (historial, P17). */
    List<PeriodoMensual> listar();

    Optional<Determinacion> buscarDeterminacion(YearMonth periodo);

    /**
     * En una sola transacción: crea o actualiza el mes (ventas y estado, no las facturas) y reemplaza su
     * determinación.
     */
    void guardar(PeriodoMensual periodo, Determinacion determinacion);
}
