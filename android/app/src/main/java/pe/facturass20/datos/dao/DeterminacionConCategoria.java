package pe.facturass20.datos.dao;

import androidx.room.Embedded;
import androidx.room.Relation;

import pe.facturass20.datos.entidades.CategoriaNrusEntity;
import pe.facturass20.datos.entidades.DeterminacionEntity;

/** Una determinación con su categoría (nula si el mes quedó fuera del régimen). */
public class DeterminacionConCategoria {

    @Embedded
    public DeterminacionEntity determinacion;

    @Relation(parentColumn = "id_categoria", entityColumn = "id_categoria")
    public CategoriaNrusEntity categoria;
}
