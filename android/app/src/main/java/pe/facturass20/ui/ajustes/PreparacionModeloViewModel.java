package pe.facturass20.ui.ajustes;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;

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
 * no hay modelo, las facturas se registran a mano (P19).
 */
public final class PreparacionModeloViewModel extends BaseViewModel {

    /** El modelo del manifiesto frente al instalado. */
    public record Modelo(GestorModeloLocal.Estado estado, String versionManifiesto, String versionActiva,
                         long tamanoBytes) { }

    /** Lo que se mide del teléfono. */
    public record Equipo(long memoria, long espacioLibre, boolean wifi, String android) {

        public boolean memoriaSuficiente() {
            return memoria >= GestorModeloLocal.MEMORIA_MINIMA;
        }

        public boolean espacioSuficiente() {
            return espacioLibre >= GestorModeloLocal.ESPACIO_MINIMO;
        }

        /**
         * Hay memoria y espacio para descargar y usar el modelo (la conexión solo importa al descargar). La
         * compatibilidad, que no cambia, la decide {@link GestorModeloLocal#telefonoCompatible()}.
         */
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
        ContenedorDependencias c = contenedor();
        enFondo(() -> new Equipo(GestorModeloLocal.memoriaTotal(contexto), c.gestorModelo().espacioLibre(),
                wifi(contexto), Build.VERSION.RELEASE), equipo::setValue);
    }

    private static boolean wifi(Context contexto) {
        ConnectivityManager conexiones = (ConnectivityManager) contexto.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities capacidades = conexiones.getNetworkCapabilities(conexiones.getActiveNetwork());
        return capacidades != null && capacidades.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }
}
