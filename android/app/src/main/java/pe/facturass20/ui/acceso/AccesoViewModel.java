package pe.facturass20.ui.acceso;

import android.app.Application;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Arrays;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.ui.comun.BaseViewModel;
import pe.facturass20.ui.comun.Evento;

/**
 * P02 Acceso (RF-19): PIN de 4 dígitos, huella y restablecer el PIN con el RUC del negocio. Tras 3 PIN
 * incorrectos hay que esperar 30 s ({@link ControlIntentos}). Al acertar desbloquea la {@link Sesion} y
 * {@code MainActivity} pasa a P04.
 */
public final class AccesoViewModel extends BaseViewModel {

    public enum Modo {
        INGRESAR,
        /** Tras comprobar el RUC: crear el PIN nuevo. */
        NUEVO_PIN,
        REPETIR_PIN
    }

    private final PreferenciasAcceso preferencias;
    private final ControlIntentos intentos;
    private final CreacionPin creacion = new CreacionPin();
    private final MutableLiveData<String> nombreNegocio = new MutableLiveData<>();
    private final MutableLiveData<Modo> modo = new MutableLiveData<>(Modo.INGRESAR);
    private final MutableLiveData<String> aviso = new MutableLiveData<>();
    private final MutableLiveData<Long> esperaHasta = new MutableLiveData<>(0L);
    private final MutableLiveData<Evento<Boolean>> pinRechazado = new MutableLiveData<>();

    public AccesoViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
        preferencias = contenedor().preferenciasAcceso();
        intentos = preferencias.controlIntentos();
        enFondo(() -> contenedor().contribuyentes().obtener().map(Contribuyente::nombre).orElse(""),
                nombreNegocio::setValue);
        comprobarEspera();
    }

    public LiveData<String> nombreNegocio() {
        return nombreNegocio;
    }

    public LiveData<Modo> modo() {
        return modo;
    }

    /** Texto bajo los puntos: PIN incorrecto, intentos que quedan, PIN que no coincide; nulo si no hay. */
    public LiveData<String> aviso() {
        return aviso;
    }

    /**
     * Hasta cuándo esperar para otro PIN, en milisegundos de {@code SystemClock.elapsedRealtime()};
     * 0 si no hay espera. La pantalla muestra la cuenta atrás y luego llama a {@link #comprobarEspera()}.
     */
    public LiveData<Long> esperaHasta() {
        return esperaHasta;
    }

    /** Para sacudir el teclado. */
    public LiveData<Evento<Boolean>> pinRechazado() {
        return pinRechazado;
    }

    public boolean huellaActivada() {
        return preferencias.huellaActivada();
    }

    /** Actualiza {@link #esperaHasta()}; al terminar la espera quita el aviso. */
    public void comprobarEspera() {
        long restantes = intentos.msRestantes();
        if (restantes > 0) {
            esperaHasta.setValue(SystemClock.elapsedRealtime() + restantes);
            aviso.setValue(texto(R.string.p02_espera));
        } else if (esperaHasta.getValue() != null && esperaHasta.getValue() != 0L) {
            esperaHasta.setValue(0L);
            aviso.setValue(null);
        }
    }

    /** Un PIN completo del teclado. */
    public void ingresarPin(char[] pin) {
        Modo actual = modo.getValue();
        if (actual == Modo.INGRESAR) {
            verificar(pin);
        } else {
            crear(pin);
        }
    }

    public void desbloquearConHuella() {
        contenedor().ejecutor().execute(intentos::reiniciar);
        contenedor().sesion().desbloquear();
    }

    /** «¿Olvidó su PIN?»: si el RUC es el del negocio, pasa a crear un PIN nuevo. */
    public void restablecer(String ruc) {
        String escrito = ruc.trim();
        enFondo(() -> contenedor().contribuyentes().obtener().map(c -> c.ruc().equals(escrito)).orElse(false),
                coincide -> {
                    if (coincide) {
                        creacion.reiniciar();
                        aviso.setValue(null);
                        modo.setValue(Modo.NUEVO_PIN);
                    } else {
                        mostrarMensaje(texto(R.string.p02_ruc_no_coincide));
                    }
                });
    }

    /** Desde el PIN nuevo vuelve a pedir el PIN de siempre. */
    public boolean cancelarRestablecer() {
        if (modo.getValue() == Modo.INGRESAR || Boolean.TRUE.equals(ocupado().getValue())) {
            return false;
        }
        creacion.reiniciar();
        aviso.setValue(null);
        modo.setValue(Modo.INGRESAR);
        return true;
    }

    private void verificar(char[] pin) {
        if (intentos.bloqueado()) {
            Arrays.fill(pin, '0');
            comprobarEspera();
            pinRechazado.setValue(new Evento<>(true));
            return;
        }
        ContenedorDependencias c = contenedor();
        enFondo(() -> {
            boolean correcto = c.gestorPin().verificar(pin);
            Arrays.fill(pin, '0');
            if (correcto) {
                intentos.reiniciar();
            } else {
                intentos.registrarFallo();
            }
            return correcto;
        }, correcto -> {
            if (correcto) {
                c.sesion().desbloquear();
                return;
            }
            pinRechazado.setValue(new Evento<>(true));
            if (intentos.bloqueado()) {
                comprobarEspera();
            } else {
                int quedan = intentos.intentosRestantes();
                aviso.setValue(getApplication().getResources().getQuantityString(R.plurals.p02_pin_incorrecto,
                        quedan, quedan));
            }
        });
    }

    private void crear(char[] pin) {
        switch (creacion.ingresar(pin)) {
            case REPETIR:
                aviso.setValue(null);
                modo.setValue(Modo.REPETIR_PIN);
                break;
            case NO_COINCIDE:
                aviso.setValue(texto(R.string.pin_no_coincide));
                pinRechazado.setValue(new Evento<>(true));
                modo.setValue(Modo.NUEVO_PIN);
                break;
            case LISTO:
                char[] nuevo = creacion.pin();
                ContenedorDependencias c = contenedor();
                enFondo(() -> {
                    c.gestorPin().establecer(nuevo);
                    intentos.reiniciar();
                    return true;
                }, listo -> {
                    creacion.reiniciar();
                    c.sesion().desbloquear();
                }, error -> {
                    creacion.reiniciar();
                    modo.setValue(Modo.NUEVO_PIN);
                    mostrarMensaje(error);
                });
                break;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        creacion.reiniciar();
    }
}
