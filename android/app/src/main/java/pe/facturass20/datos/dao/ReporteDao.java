package pe.facturass20.datos.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import pe.facturass20.datos.entidades.ReporteMensualEntity;

/** Reportes PDF generados (se usan en la iteración I5). */
@Dao
public interface ReporteDao {

    @Insert
    long insertar(ReporteMensualEntity reporte);

    @Query("SELECT * FROM reporte_mensual WHERE id_periodo = :idPeriodo ORDER BY fecha_generacion DESC LIMIT 1")
    ReporteMensualEntity ultimoDelPeriodo(long idPeriodo);
}
