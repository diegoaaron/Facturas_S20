package pe.facturass20.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static pe.facturass20.ui.SesionDePrueba.esperar;

import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.OrigenRegistro;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.ui.captura.CapturaActivity;
import pe.facturass20.ui.captura.EvaluadorNitidez;
import pe.facturass20.ui.captura.FotoCapturada;
import pe.facturass20.ui.comun.MainActivity;

/**
 * Flujo principal en el teléfono (RNF-09, cierre de I3): Escanear → disparar → Guardar, en 3 toques o menos,
 * con la guía P05 oculta, la base cifrada real y el {@code ExtractorFalso} de P07.
 *
 * <p>La cámara (P06) se reemplaza con Espresso-Intents: una prueba no puede garantizar una foto nítida
 * frente al teléfono. La respuesta simulada hace lo mismo que {@link CapturaActivity} al disparar (deja la
 * foto en {@code CapturaEnCurso} y termina con {@code RESULT_OK}) y cuenta como el toque del disparador.</p>
 *
 * <p>Usa la base de la app ({@link SesionDePrueba}). La factura queda guardada;
 * {@code connectedDebugAndroidTest} desinstala la app al terminar.</p>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class FlujoPrincipalTest {

    /** Toques que permite RNF-09 para registrar una factura. */
    private static final int TOQUES_MAXIMOS = 3;

    private ContenedorDependencias contenedor;
    private boolean omitiaGuia;
    private int toques;

    @Before
    public void prepararSesion() {
        contenedor = SesionDePrueba.preparar();
        omitiaGuia = contenedor.preferenciasCaptura().omitirGuia();
        contenedor.preferenciasCaptura().omitirGuia(true);

        Intents.init();
        intending(hasComponent(CapturaActivity.class.getName())).respondWithFunction(intent -> {
            toques++; // el disparador de P06
            contenedor.capturaEnCurso().poner(new FotoCapturada(fotoDePrueba(),
                    new EvaluadorNitidez.Evaluacion(120, 150), LocalDateTime.now(), false, null));
            return new Instrumentation.ActivityResult(Activity.RESULT_OK, null);
        });
    }

    @After
    public void restaurar() {
        Intents.release();
        contenedor.preferenciasCaptura().omitirGuia(omitiaGuia);
    }

    @Test
    public void escanearDispararYGuardarEnTresToques() {
        YearMonth mes = YearMonth.from(contenedor.reloj().hoy());
        List<FacturaCompra> antes = vigentes(mes);

        try (ActivityScenario<MainActivity> ignorado = ActivityScenario.launch(MainActivity.class)) {
            esperar(withId(R.id.boton_escanear));
            tocar(withId(R.id.boton_escanear));

            // P07 lee la foto y pasa sola a P08.
            esperar(withId(R.id.boton_guardar));
            tocar(withId(R.id.boton_guardar));

            // P10 Factura guardada.
            esperar(withId(R.id.boton_escanear_otra));
            onView(withId(R.id.texto_importe)).check(matches(isDisplayed()));
        }

        assertTrue("Se usaron " + toques + " toques", toques <= TOQUES_MAXIMOS);
        List<FacturaCompra> nuevas = vigentes(mes);
        nuevas.removeIf(f -> antes.stream().anyMatch(a -> a.id().equals(f.id())));
        assertEquals("Debe quedar una factura nueva en " + mes, 1, nuevas.size());
        FacturaCompra guardada = nuevas.get(0);
        assertEquals(OrigenRegistro.IA, guardada.origen());
        assertTrue("La foto debe guardarse cifrada", contenedor.facturas().buscarImagen(guardada.id()).isPresent());
        assertEquals(7, contenedor.facturas().buscarCampos(guardada.id()).size());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                assertNull("La captura en curso debe limpiarse", contenedor.capturaEnCurso().foto()));
    }

    private List<FacturaCompra> vigentes(YearMonth mes) {
        return contenedor.periodos().buscar(mes)
                .map(PeriodoMensual::facturasVigentes)
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
    }

    private void tocar(Matcher<View> vista) {
        onView(vista).perform(click());
        toques++;
    }

    /** Una «factura» blanca con texto, ya preparada como la deja {@code PreparadorFoto} (896 px, JPEG 90). */
    private static byte[] fotoDePrueba() {
        Bitmap imagen = Bitmap.createBitmap(672, 896, Bitmap.Config.ARGB_8888);
        Canvas lienzo = new Canvas(imagen);
        lienzo.drawColor(Color.WHITE);
        Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
        texto.setColor(Color.BLACK);
        texto.setTextSize(32);
        for (int fila = 0; fila < 20; fila++) {
            lienzo.drawText("FACTURA ELECTRÓNICA F001-" + fila, 40, 80 + fila * 40, texto);
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        imagen.compress(Bitmap.CompressFormat.JPEG, 90, salida);
        imagen.recycle();
        return salida.toByteArray();
    }
}
