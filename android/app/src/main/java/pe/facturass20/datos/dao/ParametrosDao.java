package pe.facturass20.datos.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import pe.facturass20.datos.entidades.CategoriaNrusEntity;
import pe.facturass20.datos.entidades.CronogramaVencEntity;
import pe.facturass20.datos.entidades.ParametroVersionEntity;

/** Versiones de los parámetros del NRUS con sus categorías y cronograma. */
@Dao
public interface ParametrosDao {

    @Query("SELECT * FROM parametro_version WHERE activo = 1 LIMIT 1")
    ParametroVersionEntity activa();

    @Query("SELECT * FROM parametro_version WHERE version = :version")
    ParametroVersionEntity buscar(String version);

    @Query("UPDATE parametro_version SET activo = 0")
    void desactivarTodas();

    @Insert
    long insertarVersion(ParametroVersionEntity version);

    @Insert
    void insertarCategorias(List<CategoriaNrusEntity> categorias);

    @Insert
    void insertarCronograma(List<CronogramaVencEntity> cronograma);

    @Query("SELECT * FROM categoria_nrus WHERE id_parametro = :idParametro ORDER BY limite_mensual")
    List<CategoriaNrusEntity> categorias(long idParametro);

    @Query("SELECT * FROM categoria_nrus WHERE id_parametro = :idParametro AND codigo = :codigo")
    CategoriaNrusEntity buscarCategoria(long idParametro, int codigo);

    @Query("SELECT * FROM cronograma_venc WHERE id_parametro = :idParametro ORDER BY anio, mes, ultimo_digito")
    List<CronogramaVencEntity> cronograma(long idParametro);
}
