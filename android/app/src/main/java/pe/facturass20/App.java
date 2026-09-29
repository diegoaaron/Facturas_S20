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

    private ContenedorDependencias contenedor;

    @Override
    public void onCreate() {
        super.onCreate();
        contenedor = new ContenedorDependencias(this);
        crearCanalAvisos();
    }

    public ContenedorDependencias contenedor() {
        return contenedor;
    }

    private void crearCanalAvisos() {
        NotificationChannel canal = new NotificationChannel(CANAL_AVISOS,
                getString(R.string.canal_avisos_nombre), NotificationManager.IMPORTANCE_DEFAULT);
        canal.setDescription(getString(R.string.canal_avisos_descripcion));
        getSystemService(NotificationManager.class).createNotificationChannel(canal);
    }
}
