package pe.facturass20.trabajos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationManagerCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

import pe.facturass20.App;
import pe.facturass20.ContenedorDependencias;
import pe.facturass20.dominio.casosuso.RevisarAvisos;
import pe.facturass20.dominio.modelo.Aviso;
import pe.facturass20.dominio.modelo.CategoriaNRUS;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.NivelAlerta;
import pe.facturass20.dominio.modelo.TipoAviso;

/**
 * Arma y muestra cada notificación del NRUS con datos de ejemplo (no lee ni escribe facturas). Necesita el
 * permiso de notificaciones ya concedido. Las deja en la bandeja para revisar los textos a ojo.
 */
@RunWith(AndroidJUnit4.class)
public class NotificacionesAvisosTest {

    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);
    private static final CategoriaNRUS CATEGORIA_1 = new CategoriaNRUS(1, new BigDecimal("5000.00"), new BigDecimal("20.00"));
    private static final CategoriaNRUS CATEGORIA_2 = new CategoriaNRUS(2, new BigDecimal("8000.00"), new BigDecimal("50.00"));

    private static Determinacion determinacion(CategoriaNRUS categoria, NivelAlerta nivel) {
        return new Determinacion(OCTUBRE, new BigDecimal("4100.00"), new BigDecimal("4100.00"), categoria,
                categoria == null ? null : categoria.cuota(), LocalDate.of(2026, 11, 15), nivel, false);
    }

    @Test
    public void muestraUnaNotificacionPorTipo() {
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NotificationManagerCompat notificaciones = NotificationManagerCompat.from(contexto);
        assumeTrue("Falta el permiso de notificaciones", notificaciones.areNotificationsEnabled());
        ContenedorDependencias c = ((App) contexto.getApplicationContext()).contenedor();
        LocalDate hoy = LocalDate.of(2026, 11, 14);

        Map<TipoAviso, Determinacion> casos = new HashMap<>();
        casos.put(TipoAviso.VENCIMIENTO, determinacion(CATEGORIA_1, NivelAlerta.NINGUNA));
        casos.put(TipoAviso.TOPE_80, determinacion(CATEGORIA_1, NivelAlerta.AVISO_80));
        casos.put(TipoAviso.TOPE_100, determinacion(CATEGORIA_2, NivelAlerta.LIMITE_100));
        casos.put(TipoAviso.TOPE_ANUAL, determinacion(CATEGORIA_1, NivelAlerta.NINGUNA));
        casos.forEach((tipo, d) -> RecordatorioWorker.mostrar(contexto, notificaciones, c,
                new RevisarAvisos.Pendiente(new Aviso(OCTUBRE, tipo, hoy, true), d), hoy));

        Map<Integer, String> titulos = new HashMap<>();
        for (StatusBarNotification activa : contexto.getSystemService(NotificationManager.class).getActiveNotifications()) {
            titulos.put(activa.getId(), activa.getNotification().extras.getString(Notification.EXTRA_TITLE));
        }
        assertEquals("Declaración del NRUS de octubre de 2026", titulos.get(100 + TipoAviso.VENCIMIENTO.ordinal()));
        assertEquals("Se acerca al límite de su categoría", titulos.get(100 + TipoAviso.TOPE_80.ordinal()));
        assertEquals("Pasó a la categoría 2", titulos.get(100 + TipoAviso.TOPE_100.ordinal()));
        assertEquals("Se acerca al tope anual del Nuevo RUS", titulos.get(100 + TipoAviso.TOPE_ANUAL.ordinal()));
    }
}
