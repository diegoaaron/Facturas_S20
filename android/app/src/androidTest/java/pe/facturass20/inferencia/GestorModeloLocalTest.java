package pe.facturass20.inferencia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZonedDateTime;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.entidades.ModeloLocalEntity;
import pe.facturass20.dominio.modelo.EstadoModelo;
import pe.facturass20.dominio.puertos.Reloj;

/**
 * Instalación del modelo (§6.6, pasos 4 a 6) con un «modelo» de pocos bytes y un motor falso: no hace falta
 * el modelo real de 2,6 GB.
 */
@RunWith(AndroidJUnit4.class)
public class GestorModeloLocalTest {

    private static final Reloj RELOJ = Reloj.deClock(Clock.fixed(
            ZonedDateTime.of(2026, 10, 25, 10, 0, 0, 0, Reloj.ZONA_LIMA).toInstant(), Reloj.ZONA_LIMA));
    private static final String JSON = "{\"ruc\": {\"valor\": \"20601234565\", \"confianza\": 0.9}}";

    private Context contexto;
    private BaseDatosFacturas base;
    private MotorFalso motor;
    private File carpeta;

    @Before
    public void preparar() {
        contexto = ApplicationProvider.getApplicationContext();
        byte[] clave = new byte[32];
        new SecureRandom().nextBytes(clave);
        base = BaseDatosFacturas.enMemoria(contexto, clave);
        motor = new MotorFalso();
        carpeta = new File(contexto.getFilesDir(), GestorModeloLocal.CARPETA);
        borrarCarpeta();
    }

    @After
    public void cerrar() {
        base.close();
        borrarCarpeta();
    }

    @Test
    public void instalaVerificaYActivaElModelo() throws Exception {
        byte[] contenido = "modelo de prueba 1".getBytes(StandardCharsets.UTF_8);
        GestorModeloLocal gestor = gestor("1.0.0", contenido);
        assertEquals(GestorModeloLocal.Estado.SIN_MODELO, gestor.estado());
        assertNull(gestor.extractor());

        escribir(gestor.archivoParcial(), contenido);
        gestor.instalar(null);

        assertEquals(GestorModeloLocal.Estado.LISTO, gestor.estado());
        ModeloLocalEntity activo = base.modeloDao().activo();
        assertEquals("1.0.0", activo.version);
        assertEquals(EstadoModelo.ACTIVO, activo.estado);
        assertTrue(new File(activo.ruta).isFile());
        assertFalse("El parcial ya no debe existir", gestor.archivoParcial().exists());
        assertEquals(activo.ruta, motor.ruta);
        assertNotNull(gestor.extractor());
        assertEquals("1.0.0", gestor.extractor().version());
    }

    @Test
    public void unArchivoConOtraHuellaSeBorraYNoSeActiva() throws Exception {
        GestorModeloLocal gestor = gestor("1.0.0", "el contenido esperado".getBytes(StandardCharsets.UTF_8));
        escribir(gestor.archivoParcial(), "el contenido alterado".getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class, () -> gestor.instalar(null));

        assertFalse(gestor.archivoParcial().exists());
        assertEquals(GestorModeloLocal.Estado.SIN_MODELO, gestor.estado());
        assertNull("No se debe cargar un modelo sin verificar", motor.ruta);
    }

    @Test
    public void siNoPasaLaPruebaConLaFacturaDeControlSeBorra() throws Exception {
        byte[] contenido = "modelo que no responde JSON".getBytes(StandardCharsets.UTF_8);
        GestorModeloLocal gestor = gestor("1.0.0", contenido);
        escribir(gestor.archivoParcial(), contenido);
        motor.respuesta = "No sé leer facturas.";

        assertThrows(IOException.class, () -> gestor.instalar(null));

        assertEquals(GestorModeloLocal.Estado.SIN_MODELO, gestor.estado());
        assertFalse(new File(carpeta, "1.0.0.litertlm").exists());
        assertFalse(motor.inicializado());
    }

    @Test
    public void laVersionNuevaReemplazaALaAnteriorYBorraSuArchivo() throws Exception {
        byte[] v1 = "modelo version 1".getBytes(StandardCharsets.UTF_8);
        GestorModeloLocal anterior = gestor("1.0.0", v1);
        escribir(anterior.archivoParcial(), v1);
        anterior.instalar(null);
        File archivoV1 = new File(base.modeloDao().activo().ruta);

        byte[] v2 = "modelo version 2, mejor".getBytes(StandardCharsets.UTF_8);
        GestorModeloLocal nuevo = gestor("2.0.0", v2);
        assertEquals(GestorModeloLocal.Estado.ACTUALIZACION, nuevo.estado());
        escribir(nuevo.archivoParcial(), v2);
        nuevo.instalar(null);

        assertEquals(GestorModeloLocal.Estado.LISTO, nuevo.estado());
        assertEquals("2.0.0", base.modeloDao().activo().version);
        assertEquals(EstadoModelo.NO_INSTALADO, base.modeloDao().buscarPorVersion("1.0.0").estado);
        assertFalse("El archivo de la versión anterior debe borrarse", archivoV1.exists());
    }

    private GestorModeloLocal gestor(String version, byte[] contenido) throws IOException {
        File temporal = File.createTempFile("huella", null, contexto.getCacheDir());
        escribir(temporal, contenido);
        String huella = Huella.sha256(temporal, null);
        temporal.delete();
        ManifiestoModelo manifiesto = new ManifiestoModelo(version, "https://example.org/" + version + ".litertlm",
                huella, contenido.length);
        return new GestorModeloLocal(contexto, base, RELOJ, motor, manifiesto);
    }

    private void escribir(File archivo, byte[] contenido) throws IOException {
        archivo.getParentFile().mkdirs();
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            salida.write(contenido);
        }
    }

    private void borrarCarpeta() {
        File[] archivos = carpeta.listFiles();
        if (archivos != null) {
            for (File archivo : archivos) {
                archivo.delete();
            }
        }
    }

    /** Responde siempre lo mismo y recuerda qué modelo cargó. */
    private static final class MotorFalso implements MotorInferencia {

        String ruta;
        String respuesta = JSON;

        @Override
        public void inicializar(String rutaModelo) {
            ruta = rutaModelo;
        }

        @Override
        public boolean inicializado() {
            return ruta != null;
        }

        @Override
        public String generar(byte[] jpeg, String instruccion, long timeoutMs) {
            return respuesta;
        }

        @Override
        public void close() {
            ruta = null;
        }
    }
}
