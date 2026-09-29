package pe.facturass20.datos.parametros;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.SecureRandom;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.repositorios.ParametrosRepositorioRoom;
import pe.facturass20.dominio.modelo.ConfiguracionNrusException;
import pe.facturass20.dominio.modelo.ParametrosNrus;

@RunWith(AndroidJUnit4.class)
public class CargadorParametrosNrusTest {

    private BaseDatosFacturas db;
    private CargadorParametrosNrus cargador;
    private ParametrosRepositorioRoom parametros;

    @Before
    public void abrir() {
        Context contexto = ApplicationProvider.getApplicationContext();
        byte[] clave = new byte[32];
        new SecureRandom().nextBytes(clave);
        db = BaseDatosFacturas.enMemoria(contexto, clave);
        cargador = new CargadorParametrosNrus(contexto, db);
        parametros = new ParametrosRepositorioRoom(db);
    }

    @After
    public void cerrar() {
        db.close();
    }

    @Test
    public void sinCargarNoHayParametros() {
        assertThrows(ConfiguracionNrusException.class, parametros::vigentes);
    }

    @Test
    public void unaVersionNuevaQuedaComoLaUnicaActiva() {
        assertTrue(cargador.cargarSiHaceFalta());
        ParametrosNrus actual = parametros.vigentes();
        ParametrosNrus nueva = new ParametrosNrus("2026.1", actual.vigenteDesde(), actual.umbralAviso(),
                actual.topeAnual(), actual.categorias(), actual.cronograma());

        assertTrue(cargador.cargar(nueva));
        assertFalse(cargador.cargar(nueva));
        assertEquals("2026.1", parametros.vigentes().version());
        assertTrue(db.parametrosDao().buscar("2026.1").activo);
        assertFalse(db.parametrosDao().buscar(actual.version()).activo);
    }

    @Test
    public void rechazaUnJsonIncompleto() {
        assertThrows(IllegalArgumentException.class, () -> CargadorParametrosNrus.interpretar("{\"version\":\"x\"}"));
        assertThrows(IllegalArgumentException.class, () -> CargadorParametrosNrus.interpretar(
                "{\"version\":\"x\",\"vigenteDesde\":\"2026-01-01\",\"umbralAviso\":0.8,\"topeAnual\":1,"
                        + "\"categorias\":[],\"cronograma\":[]}"));
    }
}
