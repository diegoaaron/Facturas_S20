package pe.facturass20.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static pe.facturass20.ui.SesionDePrueba.esperar;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.EnumMap;
import java.util.Map;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.ui.comun.MainActivity;
import pe.facturass20.ui.mes.PreferenciasAvisos;
import pe.facturass20.ui.mes.PreferenciasAvisos.Aviso;

/**
 * Las filas con interruptor ({@code Filas.interruptor}) comparten el id {@code interruptor}. Antes, al volver
 * a una pantalla Android les restauraba a todas el estado del último y el cambio se guardaba: en P18, ir a
 * P03 y volver activaba la huella. Aquí se prueba con los avisos de P14, que no dependen del lector de huella.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class InterruptoresTest {

    private PreferenciasAvisos avisos;
    private final Map<Aviso, Boolean> antes = new EnumMap<>(Aviso.class);

    @Before
    public void preparar() {
        ContenedorDependencias contenedor = SesionDePrueba.preparar();
        avisos = contenedor.preferenciasAvisos();
        for (Aviso aviso : Aviso.values()) {
            antes.put(aviso, avisos.activado(aviso));
        }
        // El primero apagado y el último encendido: con el error, el primero tomaba el estado del último.
        avisos.activar(Aviso.LIMITE_80, false);
        avisos.activar(Aviso.LIMITE_100, true);
        avisos.activar(Aviso.TOPE_ANUAL, true);
    }

    @After
    public void restaurar() {
        antes.forEach(avisos::activar);
    }

    @Test
    public void alVolverAP14LosAvisosConservanSuEstado() {
        String aviso80 = texto(R.string.categoria_aviso_80);
        try (ActivityScenario<MainActivity> ignorado = ActivityScenario.launch(MainActivity.class)) {
            esperar(withId(R.id.categoria));
            onView(withId(R.id.categoria)).perform(click());
            esperar(withContentDescription(aviso80));
            onView(withContentDescription(aviso80)).check(matches(isNotChecked()));

            // Hasta el final: con scrollTo la fila queda bajo el botón central Escanear y el toque abre P05.
            onView(withId(R.id.desplazamiento)).perform(swipeUp(), swipeUp());
            onView(withText(R.string.p15_titulo)).perform(click());
            esperar(withId(R.id.texto_fecha));
            pressBack();

            esperar(withContentDescription(aviso80));
            onView(withContentDescription(aviso80)).check(matches(isNotChecked()));
        }
        assertFalse(avisos.activado(Aviso.LIMITE_80));
        assertTrue(avisos.activado(Aviso.TOPE_ANUAL));
    }

    private static String texto(int id) {
        return InstrumentationRegistry.getInstrumentation().getTargetContext().getString(id);
    }
}
