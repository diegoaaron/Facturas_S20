package pe.facturass20.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;
import static pe.facturass20.ui.SesionDePrueba.esperar;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Locale;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;
import pe.facturass20.inferencia.GestorModeloLocal;
import pe.facturass20.ui.captura.CapturaActivity;
import pe.facturass20.ui.captura.EvaluadorNitidez;
import pe.facturass20.ui.captura.FotoCapturada;
import pe.facturass20.ui.comun.MainActivity;

/**
 * P05 → P07 → P08 con Gemma de verdad: la cámara se reemplaza como en {@link FlujoPrincipalTest}, pero la
 * foto es {@code assets/factura_control.jpg} y la lee el modelo instalado desde P03.
 *
 * <p>Se salta si no hay modelo instalado. Como {@code connectedDebugAndroidTest} desinstala la app (y el
 * modelo), se corre con {@code am instrument} (ver la guía del equipo, paso 10). No guarda la factura.</p>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LecturaGemmaTest {

    /** El límite de P07 más un margen. */
    private static final long ESPERA_MS = 150_000;

    private ContenedorDependencias contenedor;
    private boolean omitiaGuia;

    @Before
    public void preparar() throws IOException {
        contenedor = SesionDePrueba.preparar();
        assumeTrue("No hay modelo instalado (P03)",
                contenedor.gestorModelo().estado() == GestorModeloLocal.Estado.LISTO);
        omitiaGuia = contenedor.preferenciasCaptura().omitirGuia();
        contenedor.preferenciasCaptura().omitirGuia(true);

        byte[] factura = leer(InstrumentationRegistry.getInstrumentation().getTargetContext());
        Intents.init();
        intending(hasComponent(CapturaActivity.class.getName())).respondWithFunction(intent -> {
            contenedor.capturaEnCurso().poner(new FotoCapturada(factura,
                    new EvaluadorNitidez.Evaluacion(120, 150), LocalDateTime.now(), false, null));
            return new Instrumentation.ActivityResult(Activity.RESULT_OK, null);
        });
    }

    @After
    public void restaurar() {
        if (contenedor.gestorModelo().estado() != GestorModeloLocal.Estado.LISTO) {
            return;
        }
        Intents.release();
        contenedor.preferenciasCaptura().omitirGuia(omitiaGuia);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> contenedor.capturaEnCurso().limpiar());
    }

    @Test
    public void gemmaLeeLaFacturaDeControlEnP07() {
        try (ActivityScenario<MainActivity> ignorado = ActivityScenario.launch(MainActivity.class)) {
            esperar(withId(R.id.boton_escanear));
            onView(withId(R.id.boton_escanear)).perform(click());

            // P07 con Gemma; pasa sola a P08 cuando termina.
            esperar(withId(R.id.boton_guardar), ESPERA_MS);
        }
        ResultadoExtraccion[] resultado = new ResultadoExtraccion[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                resultado[0] = contenedor.capturaEnCurso().extraccion());
        assertNotNull(resultado[0]);
        Log.i("FacturasS20", String.format(Locale.ROOT, "P07 con el modelo %s: %d ms",
                resultado[0].versionModelo(), resultado[0].tiempoMs()));
        for (CampoExtraido campo : resultado[0].campos()) {
            Log.i("FacturasS20", "  " + campo.nombre().clave() + " = " + campo.valorLeido() + " (" + campo.confianza() + ")");
        }
        assertEquals(contenedor.gestorModelo().extractor().version(), resultado[0].versionModelo());
        assertEquals("20601234565", valor(resultado[0], CampoFactura.RUC));
        assertEquals("F001", valor(resultado[0], CampoFactura.SERIE));
        assertEquals("945.20", valor(resultado[0], CampoFactura.IMPORTE_TOTAL));
    }

    private static String valor(ResultadoExtraccion resultado, CampoFactura campo) {
        return resultado.campos().get(campo.ordinal()).valorLeido();
    }

    private static byte[] leer(Context contexto) throws IOException {
        try (InputStream entrada = contexto.getAssets().open("factura_control.jpg");
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            byte[] bloque = new byte[8192];
            for (int n; (n = entrada.read(bloque)) > 0; ) {
                salida.write(bloque, 0, n);
            }
            return salida.toByteArray();
        }
    }
}
