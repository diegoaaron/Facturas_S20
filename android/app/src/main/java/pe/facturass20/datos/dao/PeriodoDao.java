package pe.facturass20.datos.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import pe.facturass20.datos.entidades.DeterminacionEntity;
import pe.facturass20.datos.entidades.PeriodoEntity;

/** Meses y su determinación. Las facturas de cada mes se leen con {@link FacturaDao#listarPorPeriodo}. */
@Dao
public interface PeriodoDao {

    @Query("SELECT * FROM periodo WHERE id_contribuyente = :idContribuyente AND anio = :anio AND mes = :mes")
    PeriodoEntity buscar(long idContribuyente, int anio, int mes);

    @Query("SELECT * FROM periodo WHERE id_contribuyente = :idContribuyente AND anio = :anio ORDER BY mes")
    List<PeriodoEntity> listarDelAnio(long idContribuyente, int anio);

    @Query("SELECT * FROM periodo WHERE id_contribuyente = :idContribuyente ORDER BY anio DESC, mes DESC")
    List<PeriodoEntity> listar(long idContribuyente);

    @Insert
    long insertar(PeriodoEntity periodo);

    @Update
    void actualizar(PeriodoEntity periodo);

    @Transaction
    @Query("SELECT * FROM determinacion WHERE id_periodo = :idPeriodo")
    DeterminacionConCategoria buscarDeterminacion(long idPeriodo);

    /** Reemplaza la determinación del mes: {@code id_periodo} es UNIQUE. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long guardarDeterminacion(DeterminacionEntity determinacion);
}
