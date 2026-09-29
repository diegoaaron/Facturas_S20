package pe.facturass20.datos;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

import pe.facturass20.datos.dao.AvisoDao;
import pe.facturass20.datos.dao.ContribuyenteDao;
import pe.facturass20.datos.dao.FacturaDao;
import pe.facturass20.datos.dao.ModeloDao;
import pe.facturass20.datos.dao.ParametrosDao;
import pe.facturass20.datos.dao.PeriodoDao;
import pe.facturass20.datos.dao.ReporteDao;
import pe.facturass20.datos.entidades.AvisoEntity;
import pe.facturass20.datos.entidades.CampoExtraidoEntity;
import pe.facturass20.datos.entidades.CategoriaNrusEntity;
import pe.facturass20.datos.entidades.ContribuyenteEntity;
import pe.facturass20.datos.entidades.Convertidores;
import pe.facturass20.datos.entidades.CronogramaVencEntity;
import pe.facturass20.datos.entidades.DeterminacionEntity;
import pe.facturass20.datos.entidades.EmisorEntity;
import pe.facturass20.datos.entidades.FacturaCompraEntity;
import pe.facturass20.datos.entidades.ImagenFacturaEntity;
import pe.facturass20.datos.entidades.ModeloLocalEntity;
import pe.facturass20.datos.entidades.ParametroVersionEntity;
import pe.facturass20.datos.entidades.PeriodoEntity;
import pe.facturass20.datos.entidades.ReporteMensualEntity;

/**
 * La única base de datos de la app: SQLite cifrada con SQLCipher (documentación técnica §7). El esquema
 * de cada versión queda en {@code app/schemas/}; al cambiarlo se sube {@code version} y se escribe la
 * migración (nunca {@code fallbackToDestructiveMigration}: se perderían las facturas del usuario).
 */
@Database(version = 1, exportSchema = true, entities = {
        ContribuyenteEntity.class, PeriodoEntity.class, EmisorEntity.class, FacturaCompraEntity.class,
        ImagenFacturaEntity.class, CampoExtraidoEntity.class, ModeloLocalEntity.class,
        ParametroVersionEntity.class, CategoriaNrusEntity.class, CronogramaVencEntity.class,
        DeterminacionEntity.class, AvisoEntity.class, ReporteMensualEntity.class})
@TypeConverters(Convertidores.class)
public abstract class BaseDatosFacturas extends RoomDatabase {

    public static final String NOMBRE = "facturas.db";

    public abstract ContribuyenteDao contribuyenteDao();

    public abstract PeriodoDao periodoDao();

    public abstract FacturaDao facturaDao();

    public abstract ParametrosDao parametrosDao();

    public abstract ModeloDao modeloDao();

    public abstract AvisoDao avisoDao();

    public abstract ReporteDao reporteDao();

    /**
     * Abre (o crea) la base cifrada. No toca el disco hasta la primera consulta, que debe hacerse fuera
     * del hilo de UI.
     *
     * @param clave 32 bytes de {@code GestorClaves.claveBaseDatos}
     */
    public static BaseDatosFacturas abrir(Context contexto, byte[] clave, String nombre) {
        cargarSqlcipher();
        return Room.databaseBuilder(contexto.getApplicationContext(), BaseDatosFacturas.class, nombre)
                .openHelperFactory(new SupportOpenHelperFactory(clave.clone()))
                .build();
    }

    /** Base cifrada solo en memoria, para las pruebas instrumentadas. */
    public static BaseDatosFacturas enMemoria(Context contexto, byte[] clave) {
        cargarSqlcipher();
        return Room.inMemoryDatabaseBuilder(contexto.getApplicationContext(), BaseDatosFacturas.class)
                .openHelperFactory(new SupportOpenHelperFactory(clave.clone()))
                .build();
    }

    /** SQLCipher necesita su biblioteca nativa cargada antes de abrir la primera base. */
    private static void cargarSqlcipher() {
        System.loadLibrary("sqlcipher");
    }
}
