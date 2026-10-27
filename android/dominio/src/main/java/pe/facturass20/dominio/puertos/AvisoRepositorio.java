package pe.facturass20.dominio.puertos;

import java.time.YearMonth;
import java.util.List;

import pe.facturass20.dominio.modelo.Aviso;

/** Avisos ya mandados, para no repetirlos (tabla {@code aviso}). */
public interface AvisoRepositorio {

    /** Los avisos del mes; vacía si el mes no existe. */
    List<Aviso> listar(YearMonth periodo);

    /** Anota un aviso. El mes tiene que existir. */
    void registrar(Aviso aviso);
}
