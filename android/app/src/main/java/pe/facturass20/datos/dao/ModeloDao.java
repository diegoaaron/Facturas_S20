package pe.facturass20.datos.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import pe.facturass20.datos.entidades.ModeloLocalEntity;

/** Versiones del modelo Gemma instaladas (las usa {@code GestorModeloLocal} en la iteración I4). */
@Dao
public interface ModeloDao {

    @Query("SELECT * FROM modelo_local WHERE version = :version")
    ModeloLocalEntity buscarPorVersion(String version);

    @Query("SELECT * FROM modelo_local WHERE estado = 'ACTIVO' LIMIT 1")
    ModeloLocalEntity activo();

    @Insert
    long insertar(ModeloLocalEntity modelo);

    @Update
    void actualizar(ModeloLocalEntity modelo);
}
