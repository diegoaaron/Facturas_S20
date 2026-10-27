package pe.facturass20.datos;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import pe.facturass20.datos.cifrado.GestorClaves;

/**
 * Herramienta para la exposición del APF3, no una prueba del producto: copia la base real del teléfono a
 * un archivo SQLite <b>sin cifrar</b> ({@code files/facturas_legible.db}) para revisarla en DBeaver.
 * Solo lee la base; corre en el proceso de la app, que es el único que puede usar la clave del Keystore.
 * El script {@code documentos_teoricos/entregable3/entregable3_archivos/scripts/copiar_base.ps1} la ejecuta
 * y trae la copia a la PC. No se corre con {@code connectedDebugAndroidTest} (memoria 011).
 */
@RunWith(AndroidJUnit4.class)
public class CopiaLegibleBaseTest {

    public static final String NOMBRE_COPIA = "facturas_legible.db";

    @Test
    public void exportarCopiaSinCifrar() {
        Context contexto = ApplicationProvider.getApplicationContext();
        File copia = new File(contexto.getFilesDir(), NOMBRE_COPIA);
        if (copia.exists()) {
            assertTrue("No se pudo borrar la copia anterior", copia.delete());
        }
        BaseDatosFacturas base = BaseDatosFacturas.abrir(
                contexto, GestorClaves.claveBaseDatos(contexto), BaseDatosFacturas.NOMBRE);
        try {
            SupportSQLiteDatabase bd = base.getOpenHelper().getWritableDatabase();
            // Lleva al archivo principal lo que aún está en el WAL, para que también la copia cifrada esté completa.
            consultar(bd, "PRAGMA wal_checkpoint(TRUNCATE)");
            bd.execSQL("ATTACH DATABASE '" + copia.getAbsolutePath() + "' AS legible KEY ''");
            consultar(bd, "SELECT sqlcipher_export('legible')");
            bd.execSQL("DETACH DATABASE legible");
        } finally {
            base.close();
        }
        assertTrue("No se generó la copia", copia.length() > 0);
    }

    private static void consultar(SupportSQLiteDatabase bd, String sql) {
        try (Cursor cursor = bd.query(sql)) {
            cursor.moveToFirst();
        }
    }
}
