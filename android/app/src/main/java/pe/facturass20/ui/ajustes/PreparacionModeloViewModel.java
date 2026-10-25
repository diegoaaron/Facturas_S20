package pe.facturass20.ui.ajustes;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.StatFs;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import pe.facturass20.ui.comun.BaseViewModel;

/**
 * P03 Preparar lectura con IA (RF-20, documentación técnica §6.6, paso 1): comprueba si el teléfono puede
 * usar el modelo. La descarga (DescargaModeloWorker, SHA-256 y prueba) llega en la iteración I4, cuando el
 * modelo convertido esté publicado; mientras tanto la app usa el lector de prueba o el registro manual.
 */
public final class PreparacionModeloViewModel extends BaseViewModel {

    /** Los teléfonos de «6 GB» reportan algo menos de memoria total (§6.6). */
    static final long MEMORIA_MINIMA = 5_500L * 1024 * 1024;
    /** Dos veces el modelo mientras se conservan dos versiones (§6.6). */
    static final long ESPACIO_MINIMO = 3_000L * 1024 * 1024;
    /** Tamaño aproximado del modelo Gemma 4 E2B convertido (§6.6). */
    public static final long TAMANO_MODELO = 2_600L * 1024 * 1024;

    /** Lo que se mide del teléfono. */
    public record Equipo(long memoria, long espacioLibre, boolean wifi, String android) {

        public boolean memoriaSuficiente() {
            return memoria >= MEMORIA_MINIMA;
        }

        public boolean espacioSuficiente() {
            return espacioLibre >= ESPACIO_MINIMO;
        }

        /** El teléfono podrá usar la lectura con IA (la conexión solo importa al descargar). */
        public boolean apto() {
            return memoriaSuficiente() && espacioSuficiente();
        }
    }

    private final MutableLiveData<Equipo> equipo = new MutableLiveData<>();

    public PreparacionModeloViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    public LiveData<Equipo> equipo() {
        return equipo;
    }

    public void comprobar() {
        Context contexto = getApplication();
        enFondo(() -> {
            ActivityManager.MemoryInfo memoria = new ActivityManager.MemoryInfo();
            ((ActivityManager) contexto.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(memoria);
            long libre = new StatFs(contexto.getFilesDir().getAbsolutePath()).getAvailableBytes();
            return new Equipo(memoria.totalMem, libre, wifi(contexto), Build.VERSION.RELEASE);
        }, equipo::setValue);
    }

    private static boolean wifi(Context contexto) {
        ConnectivityManager conexiones = (ConnectivityManager) contexto.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities capacidades = conexiones.getNetworkCapabilities(conexiones.getActiveNetwork());
        return capacidades != null && capacidades.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }
}
