package pe.facturass20.ui.ajustes;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Arrays;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.datos.cifrado.GestorPin;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.ParametrosNrus;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.ui.comun.BaseViewModel;

/**
 * P18 Ajustes (RF-19, RF-20, RF-22): datos del negocio, PIN y huella, guía de captura, estado del lector
 * de facturas y versión de los parámetros del NRUS.
 */
public final class AjustesViewModel extends BaseViewModel {

    public record Vista(Contribuyente contribuyente, ParametrosNrus parametros) { }

    private final MutableLiveData<Vista> vista = new MutableLiveData<>();

    public AjustesViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    public LiveData<Vista> vista() {
        return vista;
    }

    public void cargar() {
        ContenedorDependencias c = contenedor();
        enFondo(() -> new Vista(c.contribuyentes().obtener()
                .orElseThrow(() -> new ReglaNegocioException(texto(R.string.error_sin_configurar))),
                c.parametros().vigentes()), vista::setValue);
    }

    public boolean huellaDisponible() {
        return contenedor().preferenciasAcceso().huellaDisponible();
    }

    public boolean huellaActivada() {
        return contenedor().preferenciasAcceso().huellaActivada();
    }

    public void activarHuella(boolean activar) {
        contenedor().preferenciasAcceso().activarHuella(activar);
    }

    public boolean mostrarGuia() {
        return !contenedor().preferenciasCaptura().omitirGuia();
    }

    public void mostrarGuia(boolean mostrar) {
        contenedor().preferenciasCaptura().omitirGuia(!mostrar);
    }

    /** Comprueba el PIN actual y guarda el nuevo; los arreglos se borran al terminar. */
    public void cambiarPin(char[] actual, char[] nuevo, char[] repetido) {
        if (!GestorPin.formatoValido(nuevo)) {
            limpiar(actual, nuevo, repetido);
            mostrarMensaje(texto(R.string.ajustes_pin_formato));
            return;
        }
        if (!Arrays.equals(nuevo, repetido)) {
            limpiar(actual, nuevo, repetido);
            mostrarMensaje(texto(R.string.pin_no_coincide));
            return;
        }
        GestorPin gestor = contenedor().gestorPin();
        enFondo(() -> {
            try {
                if (!gestor.verificar(actual)) {
                    throw new ReglaNegocioException(texto(R.string.ajustes_pin_actual_incorrecto));
                }
                gestor.establecer(nuevo);
                return true;
            } finally {
                limpiar(actual, nuevo, repetido);
            }
        }, listo -> mostrarMensaje(texto(R.string.ajustes_pin_cambiado)));
    }

    private static void limpiar(char[]... pines) {
        for (char[] pin : pines) {
            Arrays.fill(pin, '\0');
        }
    }
}
