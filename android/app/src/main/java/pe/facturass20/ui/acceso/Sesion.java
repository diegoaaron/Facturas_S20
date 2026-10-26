package pe.facturass20.ui.acceso;

import android.os.SystemClock;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

/**
 * Si la app está configurada y si ya se ingresó el PIN o la huella (RF-19). Vive mientras vive el proceso:
 * al abrir la app de nuevo, o al volver tras {@link #BLOQUEO_EN_SEGUNDO_PLANO_MS} fuera de ella, se
 * vuelve a pedir el PIN. {@code MainActivity} elige la primera pantalla según {@link #estado()}.
 *
 * <p>Se usa desde el hilo de UI.</p>
 */
public final class Sesion {

    public enum Estado {
        /** Falta la configuración inicial (P01). */
        SIN_CONFIGURAR,
        /** Hay que ingresar el PIN o la huella (P02). */
        BLOQUEADA,
        DESBLOQUEADA
    }

    /** Tiempo fuera de la app tras el que se vuelve a pedir el PIN. */
    public static final long BLOQUEO_EN_SEGUNDO_PLANO_MS = 5 * 60_000L;

    private final Executor ejecutor;
    private final BooleanSupplier hayPin;
    private final MutableLiveData<Estado> estado = new MutableLiveData<>();
    private boolean comprobando;
    private long salida = -1;
    /** Actividades de la app en pantalla; la app sale cuando llega a 0. */
    private int visibles;

    /** @param hayPin consulta la base; se llama en {@code ejecutor} */
    public Sesion(Executor ejecutor, BooleanSupplier hayPin) {
        this.ejecutor = ejecutor;
        this.hayPin = hayPin;
    }

    /** {@code null} hasta que {@link #comprobar()} consulta la base. */
    public LiveData<Estado> estado() {
        return estado;
    }

    public boolean desbloqueada() {
        return estado.getValue() == Estado.DESBLOQUEADA;
    }

    /** Averigua en segundo plano si ya hay PIN; solo la primera vez. */
    public void comprobar() {
        if (estado.getValue() != null || comprobando) {
            return;
        }
        comprobando = true;
        ejecutor.execute(() -> estado.postValue(hayPin.getAsBoolean() ? Estado.BLOQUEADA : Estado.SIN_CONFIGURAR));
    }

    /** Tras la configuración inicial, el PIN correcto, la huella o un PIN nuevo. */
    public void desbloquear() {
        estado.setValue(Estado.DESBLOQUEADA);
    }

    /**
     * Una actividad dejó de verse ({@code onStop}, también al girar). Solo cuenta como salir de la app si no
     * queda otra a la vista: al abrir la cámara, {@code MainActivity} se detiene después de que
     * {@code CapturaActivity} ya empezó.
     */
    public void alSalir() {
        visibles = Math.max(0, visibles - 1);
        if (visibles == 0) {
            salida = SystemClock.elapsedRealtime();
        }
    }

    /** Una actividad se ve ({@code onStart}): si la app estuvo fuera demasiado tiempo, se bloquea. */
    public void alVolver() {
        visibles++;
        if (desbloqueada() && salida >= 0
                && SystemClock.elapsedRealtime() - salida >= BLOQUEO_EN_SEGUNDO_PLANO_MS) {
            estado.setValue(Estado.BLOQUEADA);
        }
        salida = -1;
    }
}
