package pe.facturass20.ui.verificacion;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;
import pe.facturass20.dominio.puertos.ExtraccionException;
import pe.facturass20.ui.captura.FotoCapturada;
import pe.facturass20.ui.comun.BaseViewModel;

/**
 * P07 Lectura (RF-04): pasa la foto de {@code CapturaEnCurso} al extractor en el hilo de inferencia, con
 * un límite de 20 s, y deja el resultado en {@code CapturaEnCurso} para P08.
 *
 * <p>El extractor no informa su avance, así que el porcentaje es una estimación por tiempo que se detiene
 * en 95 % hasta que llega la respuesta.</p>
 */
public final class LecturaViewModel extends BaseViewModel {

    public enum Estado { LEYENDO, LISTA, FALLO }

    /** Límite de la lectura (documentación técnica §8.1, P07; RNF-04). */
    static final long LIMITE_MS = 20_000;
    /** Duración que se muestra; se ajustará con la medición de Gemma en el teléfono de referencia. */
    static final long ESTIMADO_MS = 4_000;
    private static final long INTERVALO_MS = 100;

    private final MutableLiveData<Estado> estado = new MutableLiveData<>();
    private final MutableLiveData<Integer> progreso = new MutableLiveData<>(0);
    private final MutableLiveData<Long> segundosRestantes = new MutableLiveData<>();
    private final Handler hiloUi = new Handler(Looper.getMainLooper());
    private final Runnable avanzar = this::avanzar;
    private String error;
    private Future<ResultadoExtraccion> tarea;
    private long inicio;
    private boolean cancelada;

    public LecturaViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    /** Nulo hasta {@link #iniciar()}. */
    public LiveData<Estado> estado() {
        return estado;
    }

    /** De 0 a 100. */
    public LiveData<Integer> progreso() {
        return progreso;
    }

    /** Segundos estimados que faltan; 0 si ya pasó lo estimado. */
    public LiveData<Long> segundosRestantes() {
        return segundosRestantes;
    }

    /** El motivo para el usuario cuando el estado es {@link Estado#FALLO}. */
    public String error() {
        return error;
    }

    /** Empieza la lectura una sola vez, aunque la pantalla se recree. */
    public void iniciar() {
        if (estado.getValue() != null) {
            return;
        }
        ContenedorDependencias c = contenedor();
        FotoCapturada foto = c.capturaEnCurso().foto();
        if (foto == null) {
            fallar(texto(R.string.lectura_sin_foto));
            return;
        }
        estado.setValue(Estado.LEYENDO);
        inicio = SystemClock.elapsedRealtime();
        hiloUi.post(avanzar);
        tarea = c.ejecutorInferencia().submit(() -> c.extractor().extraer(foto.jpeg()));
        Future<ResultadoExtraccion> enCurso = tarea;
        enFondo(() -> esperar(enCurso), resultado -> {
            if (resultado == null) {
                return;
            }
            hiloUi.removeCallbacks(avanzar);
            c.capturaEnCurso().ponerExtraccion(resultado);
            progreso.setValue(100);
            estado.setValue(Estado.LISTA);
        }, this::fallar);
    }

    /** El usuario tocó Cancelar o Atrás; la foto se conserva por si quiere registrarla a mano. */
    public void cancelar() {
        cancelada = true;
        hiloUi.removeCallbacks(avanzar);
        if (tarea != null) {
            tarea.cancel(true);
        }
    }

    private ResultadoExtraccion esperar(Future<ResultadoExtraccion> enCurso) throws Exception {
        try {
            return enCurso.get(LIMITE_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            enCurso.cancel(true);
            throw new ReglaNegocioException(texto(R.string.lectura_tiempo_agotado));
        } catch (CancellationException e) {
            return null;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ExtraccionException) {
                throw new ReglaNegocioException(e.getCause().getMessage());
            }
            throw e;
        }
    }

    private void fallar(String motivo) {
        hiloUi.removeCallbacks(avanzar);
        if (cancelada) {
            return;
        }
        error = motivo;
        estado.setValue(Estado.FALLO);
    }

    private void avanzar() {
        long transcurrido = SystemClock.elapsedRealtime() - inicio;
        progreso.setValue((int) Math.min(95, transcurrido * 95 / ESTIMADO_MS));
        segundosRestantes.setValue(Math.max(0, (ESTIMADO_MS - transcurrido + 999) / 1000));
        hiloUi.postDelayed(avanzar, INTERVALO_MS);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelar();
    }
}
