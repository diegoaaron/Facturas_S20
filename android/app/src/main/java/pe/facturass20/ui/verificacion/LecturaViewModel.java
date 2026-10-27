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
import pe.facturass20.dominio.puertos.ExtractorFacturas;
import pe.facturass20.dominio.reglas.QrSunat;
import pe.facturass20.ui.captura.FotoCapturada;
import pe.facturass20.ui.comun.BaseViewModel;

/**
 * P07 Lectura (RF-04): pasa la foto de {@code CapturaEnCurso} al extractor en el hilo de inferencia, con
 * un límite de 2 min, corrige con el QR del comprobante lo que este traiga y deja el resultado en
 * {@code CapturaEnCurso} para P08.
 *
 * <p>El extractor no informa su avance, así que el porcentaje es una estimación por tiempo que se detiene
 * en 95 % hasta que llega la respuesta.</p>
 */
public final class LecturaViewModel extends BaseViewModel {

    public enum Estado { LEYENDO, LISTA, FALLO }

    /**
     * Límite de toda la lectura, carga del modelo incluida (§8.1, P07). RNF-04 pide 20 s, pero Gemma tarda
     * cerca de un minuto en el Moto G34 (§6.5); el extractor corta antes la generación, a los 90 s.
     */
    static final long LIMITE_MS = 120_000;
    private static final long INTERVALO_MS = 100;

    private final MutableLiveData<Estado> estado = new MutableLiveData<>();
    private final MutableLiveData<Integer> progreso = new MutableLiveData<>(0);
    private final MutableLiveData<Long> segundosRestantes = new MutableLiveData<>();
    private final MutableLiveData<String> versionModelo = new MutableLiveData<>();
    private final Handler hiloUi = new Handler(Looper.getMainLooper());
    private final Runnable avanzar = this::avanzar;
    private String error;
    private Future<ResultadoExtraccion> tarea;
    private long inicio;
    /** La duración que se muestra; se conoce al saber qué extractor lee (Gemma o el de prueba). */
    private volatile long estimadoMs = 60_000;
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

    /** La versión de Gemma que lee la foto; null mientras no se sabe o si es el lector de prueba. */
    public LiveData<String> versionModelo() {
        return versionModelo;
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
        tarea = c.ejecutorInferencia().submit(() -> {
            ExtractorFacturas extractor = c.extractor();
            estimadoMs = ContenedorDependencias.duracionEstimada(extractor);
            versionModelo.postValue(ContenedorDependencias.versionModelo(extractor));
            ResultadoExtraccion leido = extractor.extraer(foto.jpeg());
            return foto.qr() == null ? leido : QrSunat.completar(leido, foto.qr());
        });
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
        long estimado = estimadoMs;
        progreso.setValue((int) Math.min(95, transcurrido * 95 / estimado));
        segundosRestantes.setValue(Math.max(0, (estimado - transcurrido + 999) / 1000));
        hiloUi.postDelayed(avanzar, INTERVALO_MS);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelar();
    }
}
