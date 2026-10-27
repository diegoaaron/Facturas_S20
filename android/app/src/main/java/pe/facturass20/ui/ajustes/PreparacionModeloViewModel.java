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
import androidx.work.WorkInfo;

import java.util.List;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.datos.entidades.ModeloLocalEntity;
import pe.facturass20.inferencia.GestorModeloLocal;
import pe.facturass20.trabajos.DescargaModeloWorker;
import pe.facturass20.ui.comun.BaseViewModel;

/**
 * P03 Preparar lectura con IA (RF-20, documentación técnica §6.6): comprueba si el teléfono puede usar el
 * modelo, muestra cuál está instalado y lanza o cancela la descarga ({@link DescargaModeloWorker}). Mientras
 * no hay modelo, la app usa el lector de prueba o el registro manual.
 */
public final class PreparacionModeloViewModel extends BaseViewModel {

    /** Los teléfonos de «6 GB» reportan algo menos de memoria total (§6.6). */
    static final long MEMORIA_MINIMA = 5_500L * 1024 * 1024;
    /** El modelo afinado (4,9 GiB) más las cachés que LiteRT-LM crea al cargarlo (~2,3 GB) (§6.6). */
    static final long ESPACIO_MINIMO = 8_000L * 1024 * 1024;

    /** El modelo del manifiesto frente al instalado. */
    public record Modelo(GestorModeloLocal.Estado estado, String versionManifiesto, String versionActiva,
                         long tamanoBytes) { }

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
    private final MutableLiveData<Modelo> modelo = new MutableLiveData<>();

    public PreparacionModeloViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    public LiveData<Equipo> equipo() {
        return equipo;
    }

    public LiveData<Modelo> modelo() {
        return modelo;
    }

    /** La descarga en curso o la última, con su avance. */
    public LiveData<List<WorkInfo>> descarga() {
        return DescargaModeloWorker.estado(getApplication());
    }

    public void descargar() {
        DescargaModeloWorker.encolar(getApplication());
    }

    public void cancelarDescarga() {
        DescargaModeloWorker.cancelar(getApplication());
    }

    /** Vuelve a leer qué modelo está instalado (al abrir y al terminar una descarga). */
    public void comprobarModelo() {
        ContenedorDependencias c = contenedor();
        enFondo(() -> {
            GestorModeloLocal gestor = c.gestorModelo();
            ModeloLocalEntity activo = gestor.activo();
            return new Modelo(gestor.estado(), gestor.manifiesto().version(), activo == null ? null : activo.version,
                    gestor.manifiesto().tamanoBytes());
        }, modelo::setValue);
    }

    public void comprobar() {
        comprobarModelo();
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
