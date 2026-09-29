package pe.facturass20.ui.comun;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import pe.facturass20.App;
import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.ConfiguracionNrusException;
import pe.facturass20.dominio.modelo.ReglaNegocioException;

/**
 * Base de los ViewModels de las pantallas (documentación técnica §3.1): les da el contenedor de
 * dependencias, corre el trabajo con la base o el modelo en un hilo de fondo y entrega el resultado en
 * el hilo de UI.
 *
 * <p>Si la tarea falla, el usuario ve un mensaje en español en {@link #mensaje()}, nunca un código de
 * error: el de la {@link ReglaNegocioException} o uno genérico. Los Fragments se crean con
 * {@code new ViewModelProvider(this).get(...)}; basta un constructor que reciba la {@link Application}.</p>
 */
public abstract class BaseViewModel extends AndroidViewModel {

    private static final String ETIQUETA = "FacturasS20";

    private final ContenedorDependencias contenedor;
    private final Executor hiloUi;
    private final MutableLiveData<Boolean> ocupado = new MutableLiveData<>(false);
    private final MutableLiveData<Evento<String>> mensaje = new MutableLiveData<>();
    private int tareasEnCurso;
    private boolean terminado;

    protected BaseViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
        contenedor = ((App) aplicacion).contenedor();
        hiloUi = ContextCompat.getMainExecutor(aplicacion);
    }

    /** {@code true} mientras alguna tarea de fondo no termina; sirve para mostrar el progreso o bloquear botones. */
    public LiveData<Boolean> ocupado() {
        return ocupado;
    }

    /** Mensajes para mostrar en un Snackbar. */
    public LiveData<Evento<String>> mensaje() {
        return mensaje;
    }

    protected final ContenedorDependencias contenedor() {
        return contenedor;
    }

    protected final String texto(@StringRes int id, Object... argumentos) {
        return getApplication().getString(id, argumentos);
    }

    protected final void mostrarMensaje(String texto) {
        mensaje.setValue(new Evento<>(texto));
    }

    /** Como {@link #enFondo(Callable, Consumer, Consumer)}, mostrando el error en {@link #mensaje()}. */
    protected final <T> void enFondo(Callable<T> tarea, Consumer<T> alTerminar) {
        enFondo(tarea, alTerminar, this::mostrarMensaje);
    }

    /**
     * Corre {@code tarea} en el ejecutor de la app. Se llama desde el hilo de UI; {@code alTerminar} y
     * {@code alFallar} también corren en él, salvo que la pantalla ya se haya cerrado.
     *
     * @param alFallar recibe el mensaje para el usuario
     */
    protected final <T> void enFondo(Callable<T> tarea, Consumer<T> alTerminar, Consumer<String> alFallar) {
        cambiarTareas(+1);
        contenedor.ejecutor().execute(() -> {
            try {
                T resultado = tarea.call();
                hiloUi.execute(() -> terminar(() -> alTerminar.accept(resultado)));
            } catch (Exception e) {
                String texto = mensajeDe(e);
                hiloUi.execute(() -> terminar(() -> alFallar.accept(texto)));
            }
        });
    }

    private void terminar(Runnable entrega) {
        cambiarTareas(-1);
        if (!terminado) {
            entrega.run();
        }
    }

    private void cambiarTareas(int cambio) {
        tareasEnCurso += cambio;
        ocupado.setValue(tareasEnCurso > 0);
    }

    private String mensajeDe(Exception e) {
        if (e instanceof ReglaNegocioException) {
            return e.getMessage();
        }
        // No se registra el mensaje: podría traer un RUC o un monto (documentación técnica §10).
        Log.e(ETIQUETA, "Falló una tarea de " + getClass().getSimpleName() + ": " + e.getClass().getName());
        if (e instanceof ConfiguracionNrusException) {
            return texto(R.string.error_parametros_nrus);
        }
        return texto(R.string.error_generico);
    }

    @Override
    protected void onCleared() {
        terminado = true;
    }
}
