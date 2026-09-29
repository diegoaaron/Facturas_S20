package pe.facturass20.datos.cifrado;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.time.LocalDate;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.entidades.ContribuyenteEntity;

/** Claves del Keystore, fotos cifradas, PIN y el archivo de la base cifrado de verdad. */
@RunWith(AndroidJUnit4.class)
public class CifradoTest {

    private final Context contexto = ApplicationProvider.getApplicationContext();

    @Test
    public void laClaveDeLaBaseEsSiempreLaMisma() {
        byte[] primera = GestorClaves.claveBaseDatos(contexto);
        assertEquals(32, primera.length);
        assertArrayEquals(primera, GestorClaves.claveBaseDatos(contexto));
    }

    @Test
    public void lasFotosSeGuardanCifradasYSeRecuperan() throws Exception {
        File carpeta = new File(contexto.getCacheDir(), "prueba_imagenes");
        AlmacenImagenes almacen = new AlmacenImagenes(carpeta, GestorClaves::claveImagenes);
        byte[] jpeg = "FOTO DE LA FACTURA F001-004821 TOTAL S/ 1450,00".getBytes(StandardCharsets.UTF_8);

        String nombre = almacen.guardar(jpeg);
        byte[] enDisco = Files.readAllBytes(almacen.archivo(nombre).toPath());
        assertFalse(new String(enDisco, StandardCharsets.ISO_8859_1).contains("F001-004821"));
        assertEquals(12 + jpeg.length + 16, enDisco.length);
        assertArrayEquals(jpeg, almacen.leer(nombre));
        assertNotEquals(nombre, almacen.guardar(jpeg));

        enDisco[20] ^= 1;
        Files.write(almacen.archivo(nombre).toPath(), enDisco);
        assertThrows(IllegalStateException.class, () -> almacen.leer(nombre));
        assertTrue(almacen.borrar(nombre));
        assertThrows(IllegalArgumentException.class, () -> almacen.leer("../facturas.db"));
    }

    @Test
    public void elPinSeGuardaComoHashYSeVerifica() {
        BaseDatosFacturas db = BaseDatosFacturas.enMemoria(contexto, claveAleatoria());
        try {
            GestorPin pin = new GestorPin(db.contribuyenteDao());
            assertThrows(IllegalStateException.class, () -> pin.establecer("1234".toCharArray()));
            db.contribuyenteDao().insertar(contribuyente());
            assertFalse(pin.tienePin());
            assertFalse(pin.verificar("1234".toCharArray()));
            assertThrows(IllegalArgumentException.class, () -> pin.establecer("12a4".toCharArray()));

            pin.establecer("1234".toCharArray());
            assertTrue(pin.tienePin());
            assertTrue(pin.verificar("1234".toCharArray()));
            assertFalse(pin.verificar("4321".toCharArray()));
            ContribuyenteEntity fila = db.contribuyenteDao().obtener();
            assertFalse(fila.pinHash.contains("1234"));
            assertEquals(24, fila.pinSal.length());
        } finally {
            db.close();
        }
    }

    @Test
    public void elArchivoDeLaBaseNoSeLeeSinLaClave() throws Exception {
        String nombre = "prueba_cifrada.db";
        contexto.deleteDatabase(nombre);
        byte[] clave = claveAleatoria();
        BaseDatosFacturas db = BaseDatosFacturas.abrir(contexto, clave, nombre);
        db.contribuyenteDao().insertar(contribuyente());
        db.close();

        byte[] cabecera = new byte[16];
        try (java.io.FileInputStream entrada = new java.io.FileInputStream(contexto.getDatabasePath(nombre))) {
            assertEquals(16, entrada.read(cabecera));
        }
        assertNotEquals("SQLite format 3\u0000", new String(cabecera, StandardCharsets.US_ASCII));

        BaseDatosFacturas otraClave = BaseDatosFacturas.abrir(contexto, claveAleatoria(), nombre);
        assertThrows(RuntimeException.class, () -> otraClave.contribuyenteDao().obtener());
        otraClave.close();

        BaseDatosFacturas mismaClave = BaseDatosFacturas.abrir(contexto, clave, nombre);
        assertEquals("Bodega Rosita", mismaClave.contribuyenteDao().obtener().nombre);
        mismaClave.close();
        contexto.deleteDatabase(nombre);
    }

    private static byte[] claveAleatoria() {
        byte[] clave = new byte[32];
        new SecureRandom().nextBytes(clave);
        return clave;
    }

    private static ContribuyenteEntity contribuyente() {
        ContribuyenteEntity c = new ContribuyenteEntity();
        c.ruc = "10456789124";
        c.nombre = "Bodega Rosita";
        c.titular = "Rosa Quispe";
        c.ultimoDigito = 4;
        c.fechaAlta = LocalDate.of(2026, 9, 1);
        return c;
    }
}
