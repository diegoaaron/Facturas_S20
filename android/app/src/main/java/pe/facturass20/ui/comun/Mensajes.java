package pe.facturass20.ui.comun;

import android.view.View;

import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import pe.facturass20.R;

/**
 * Muestra en un Snackbar los mensajes de un {@link BaseViewModel}, una sola vez cada uno. En las pantallas
 * con barra inferior, el Snackbar sale encima de ella para no tapar la navegación.
 */
public final class Mensajes {

    private Mensajes() { }

    public static void observar(Fragment pantalla, BaseViewModel modelo, View raiz) {
        modelo.mensaje().observe(pantalla.getViewLifecycleOwner(), evento -> {
            String texto = evento.tomar();
            if (texto != null) {
                Snackbar aviso = Snackbar.make(raiz, texto, Snackbar.LENGTH_LONG);
                View barra = pantalla.requireActivity().findViewById(R.id.barra_inferior);
                if (barra != null && barra.getVisibility() == View.VISIBLE) {
                    aviso.setAnchorView(barra);
                }
                aviso.show();
            }
        });
    }
}
