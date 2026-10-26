package pe.facturass20.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import android.os.SystemClock;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;

import pe.facturass20.App;
import pe.facturass20.ContenedorDependencias;
import pe.facturass20.dominio.modelo.Contribuyente;

/** Lo que comparten las pruebas de pantallas: la app configurada y desbloqueada, y esperar a una vista. */
final class SesionDePrueba {

    /** P07 tarda 3 s con el extractor falso; el límite real de la lectura es 20 s. */
    private static final long ESPERA_MS = 25_000;
    private static final String RUC_PRUEBA = "20100070970";

    private SesionDePrueba() { }

    /**
     * Usa la base de la app: si falta la configuración inicial, crea un negocio de prueba. Deja la sesión
     * desbloqueada, así {@code MainActivity} abre en P04. Se llama desde el hilo de la prueba, nunca el de UI.
     */
    static ContenedorDependencias preparar() {
        ContenedorDependencias contenedor = ((App) ApplicationProvider.getApplicationContext()).contenedor();
        if (!contenedor.contribuyentes().obtener().isPresent()) {
            contenedor.baseDatos().runInTransaction(() -> {
                contenedor.contribuyentes().guardar(new Contribuyente(RUC_PRUEBA, "Bodega de prueba",
                        "Titular de prueba", contenedor.reloj().hoy()));
                contenedor.gestorPin().establecer("1234".toCharArray());
            });
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> contenedor.sesion().desbloquear());
        return contenedor;
    }

    /** Espera a que la vista aparezca: Espresso no sabe de los hilos de fondo de la base ni de la lectura. */
    static void esperar(Matcher<View> vista) {
        long limite = SystemClock.elapsedRealtime() + ESPERA_MS;
        while (true) {
            try {
                onView(vista).check(matches(isDisplayed()));
                return;
            } catch (NoMatchingViewException | AssertionError e) {
                if (SystemClock.elapsedRealtime() > limite) {
                    throw e;
                }
                SystemClock.sleep(200);
            }
        }
    }
}
