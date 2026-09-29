package pe.facturass20;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.util.Log;

/**
 * Punto de entrada de la app: crea el contenedor de dependencias y los canales de notificación.
 */
public class App extends Application {

    /** Canal de los avisos del NRUS: 80 % y 100 % del límite, y vencimiento de la declaración. */
    public static final String CANAL_AVISOS = "avisos_nrus";

    private static final String ETIQUETA = "FacturasS20";

    private ContenedorDependencias contenedor;

    @Override
    public void onCreate() {
        super.onCreate();
        contenedor = new ContenedorDependencias(this);
        crearCanalAvisos();
        contenedor.ejecutor().execute(this::cargarParametrosNrus);
    }

    /** Abre la base (la crea la primera vez) y carga los parámetros del NRUS si la versión es nueva. */
    private void cargarParametrosNrus() {
        try {
            if (contenedor.cargadorParametros().cargarSiHaceFalta()) {
                Log.i(ETIQUETA, "Parámetros del NRUS actualizados");
            }
        } catch (RuntimeException e) {
            Log.e(ETIQUETA, "No se pudieron cargar los parámetros del NRUS", e);
        }
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
