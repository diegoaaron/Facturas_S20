package pe.facturass20.datos.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import pe.facturass20.datos.entidades.AvisoEntity;

/** Avisos programados (los usa WorkManager en la iteración I5). */
@Dao
public interface AvisoDao {

    @Insert
    long insertar(AvisoEntity aviso);

    @Query("SELECT * FROM aviso WHERE id_periodo = :idPeriodo ORDER BY fecha_programada")
    List<AvisoEntity> listarPorPeriodo(long idPeriodo);

    @Query("UPDATE aviso SET enviado = 1 WHERE id_aviso = :idAviso")
    int marcarEnviado(long idAviso);
}
