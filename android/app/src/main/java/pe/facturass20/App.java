package pe.facturass20;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

/**
 * Punto de entrada de la app: crea el contenedor de dependencias y los canales de notificación. La base
 * se abre (y se cargan los parámetros del NRUS) cuando {@code MainActivity} consulta la sesión.
 */
public class App extends Application {

    /** Canal de los avisos del NRUS: 80 % y 100 % del límite, y vencimiento de la declaración. */
    public static final String CANAL_AVISOS = "avisos_nrus";
    /** Canal de la notificación de avance mientras se descarga el modelo (§6.6). */
    public static final String CANAL_DESCARGA = "descarga_modelo";

    private ContenedorDependencias contenedor;

    @Override
    public void onCreate() {
        super.onCreate();
        contenedor = new ContenedorDependencias(this);
        crearCanales();
    }

    public ContenedorDependencias contenedor() {
        return contenedor;
    }

    private void crearCanales() {
        NotificationChannel avisos = new NotificationChannel(CANAL_AVISOS,
                getString(R.string.canal_avisos_nombre), NotificationManager.IMPORTANCE_DEFAULT);
        avisos.setDescription(getString(R.string.canal_avisos_descripcion));
        NotificationChannel descarga = new NotificationChannel(CANAL_DESCARGA,
                getString(R.string.canal_descarga_nombre), NotificationManager.IMPORTANCE_LOW);
        descarga.setDescription(getString(R.string.canal_descarga_descripcion));
        NotificationManager notificaciones = getSystemService(NotificationManager.class);
        notificaciones.createNotificationChannel(avisos);
        notificaciones.createNotificationChannel(descarga);
    }
}
