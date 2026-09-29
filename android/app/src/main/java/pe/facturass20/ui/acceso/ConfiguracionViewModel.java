package pe.facturass20.ui.acceso;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.reglas.ValidadorRuc;
import pe.facturass20.ui.comun.BaseViewModel;
import pe.facturass20.ui.comun.Evento;

/**
 * P01 Configuración inicial (RF-19, RF-06), en tres pasos: 1) RUC, nombre del negocio y titular;
 * 2) crear el PIN; 3) repetirlo. Al terminar guarda el contribuyente y el PIN en una sola transacción,
 * ofrece activar la huella (si el teléfono la tiene) y desbloquea la {@link Sesion}: {@code MainActivity}
 * pasa entonces a P04.
 */
public final class ConfiguracionViewModel extends BaseViewModel {

    public static final int PASO_NEGOCIO = 1;
    public static final int PASO_PIN = 2;
    public static final int PASO_REPETIR_PIN = 3;

    /** Errores del formulario del paso 1; 0 si el campo está bien. */
    public record ErroresNegocio(@StringRes int ruc, @StringRes int nombre, @StringRes int titular) {

        boolean hayErrores() {
            return ruc != 0 || nombre != 0 || titular != 0;
        }
    }

    private final MutableLiveData<Integer> paso = new MutableLiveData<>(PASO_NEGOCIO);
    private final MutableLiveData<ErroresNegocio> errores = new MutableLiveData<>(new ErroresNegocio(0, 0, 0));
    private final MutableLiveData<Evento<Integer>> avisoPin = new MutableLiveData<>();
    private final MutableLiveData<Boolean> ofrecerHuella = new MutableLiveData<>(false);
    private final CreacionPin creacion = new CreacionPin();
    private Contribuyente contribuyente;

    public ConfiguracionViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    public LiveData<Integer> paso() {
        return paso;
    }

    public LiveData<ErroresNegocio> errores() {
        return errores;
    }

    /** Texto que acompaña a la sacudida del teclado cuando el PIN repetido no coincide. */
    public LiveData<Evento<Integer>> avisoPin() {
        return avisoPin;
    }

    /**
     * {@code true} cuando ya se guardó todo y falta preguntar por la huella. Es estado y no evento para
     * que el diálogo vuelva a aparecer si se gira la pantalla.
     */
    public LiveData<Boolean> ofrecerHuella() {
        return ofrecerHuella;
    }

    /** Error del RUC mientras se escribe: solo cuando ya tiene 11 dígitos, para no molestar antes. */
    @StringRes
    public static int errorRucAlEscribir(String ruc) {
        return ruc.length() == 11 && !ValidadorRuc.esValido(ruc) ? R.string.p01_error_ruc : 0;
    }

    /** Paso 1 → 2. */
    public void continuar(String ruc, String nombre, String titular) {
        ErroresNegocio encontrados = new ErroresNegocio(errorRuc(ruc.trim()),
                nombre.trim().isEmpty() ? R.string.p01_error_nombre : 0,
                titular.trim().isEmpty() ? R.string.p01_error_titular : 0);
        errores.setValue(encontrados);
        if (encontrados.hayErrores()) {
            return;
        }
        contribuyente = new Contribuyente(ruc.trim(), nombre, titular, contenedor().reloj().hoy());
        creacion.reiniciar();
        paso.setValue(PASO_PIN);
    }

    /** Desde el paso 2 o 3 vuelve al formulario, que conserva lo escrito. */
    public boolean volver() {
        Integer actual = paso.getValue();
        if (actual == null || actual == PASO_NEGOCIO || Boolean.TRUE.equals(ocupado().getValue())) {
            return false;
        }
        creacion.reiniciar();
        paso.setValue(actual == PASO_REPETIR_PIN ? PASO_PIN : PASO_NEGOCIO);
        return true;
    }

    /** Un PIN completo del teclado, en el paso 2 o 3. */
    public void ingresarPin(char[] pin) {
        switch (creacion.ingresar(pin)) {
            case REPETIR:
                paso.setValue(PASO_REPETIR_PIN);
                break;
            case NO_COINCIDE:
                avisoPin.setValue(new Evento<>(R.string.pin_no_coincide));
                paso.setValue(PASO_PIN);
                break;
            case LISTO:
                guardar(creacion.pin());
                break;
        }
    }

    /** Después de guardar: activa o no la huella y abre la app. */
    public void terminar(boolean activarHuella) {
        ofrecerHuella.setValue(false);
        contenedor().preferenciasAcceso().activarHuella(activarHuella);
        contenedor().sesion().desbloquear();
    }

    private void guardar(char[] pin) {
        ContenedorDependencias c = contenedor();
        Contribuyente datos = contribuyente;
        enFondo(() -> {
            c.baseDatos().runInTransaction(() -> {
                c.contribuyentes().guardar(datos);
                c.gestorPin().establecer(pin);
            });
            c.preferenciasAcceso().controlIntentos().reiniciar();
            return c.preferenciasAcceso().huellaDisponible();
        }, huellaDisponible -> {
            creacion.reiniciar();
            if (huellaDisponible) {
                ofrecerHuella.setValue(true);
            } else {
                terminar(false);
            }
        }, error -> {
            creacion.reiniciar();
            paso.setValue(PASO_PIN);
            mostrarMensaje(error);
        });
    }

    @StringRes
    private static int errorRuc(String ruc) {
        if (!ruc.matches("\\d{11}")) {
            return R.string.p01_error_ruc_largo;
        }
        return ValidadorRuc.esValido(ruc) ? 0 : R.string.p01_error_ruc;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        creacion.reiniciar();
    }
}
