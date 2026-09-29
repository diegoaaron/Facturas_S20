package pe.facturass20.datos.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import pe.facturass20.datos.entidades.ContribuyenteEntity;

/** Contribuyente y su PIN. */
@Dao
public interface ContribuyenteDao {

    /** El único contribuyente, o nulo antes de la configuración inicial. */
    @Query("SELECT * FROM contribuyente ORDER BY id_contribuyente LIMIT 1")
    ContribuyenteEntity obtener();

    @Insert
    long insertar(ContribuyenteEntity contribuyente);

    @Update
    void actualizar(ContribuyenteEntity contribuyente);

    @Query("UPDATE contribuyente SET pin_hash = :hash, pin_sal = :sal WHERE id_contribuyente = :id")
    int guardarPin(long id, String hash, String sal);
}
