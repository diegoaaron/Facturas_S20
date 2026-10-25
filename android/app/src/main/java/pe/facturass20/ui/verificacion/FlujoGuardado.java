package pe.facturass20.ui.verificacion;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.YearMonth;
import java.util.function.Consumer;

import pe.facturass20.App;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.ui.comun.Formatos;

/**
 * Lo que P08 y P19 hacen igual después de tocar Guardar: pintar los errores, abrir P09 si la factura ya
 * existe, preguntar por un mes anterior e ir a P10 cuando se guarda.
 */
public final class FlujoGuardado {

    private FlujoGuardado() { }

    /**
     * @param destino        el destino de la pantalla, para no navegar dos veces
     * @param accionGuardada acción hacia P10
     * @param accionDetalle  acción hacia P12, para «Ver la registrada»
     * @param textoDescartar texto del segundo botón de P09
     * @param guardar        vuelve a guardar; recibe si el usuario confirmó el mes
     */
    public static void conectar(Fragment pantalla, RegistroFacturaViewModel modelo, CamposFactura campos,
            @IdRes int destino, @IdRes int accionGuardada, @IdRes int accionDetalle, @StringRes int textoDescartar,
            Consumer<Boolean> guardar) {
        modelo.errores().observe(pantalla.getViewLifecycleOwner(), campos::mostrarErrores);
        modelo.duplicado().observe(pantalla.getViewLifecycleOwner(), evento -> {
            FacturaCompra existente = evento.tomar();
            if (existente != null) {
                DuplicadoDialog.mostrar(pantalla, existente, textoDescartar);
            }
        });
        modelo.confirmarPeriodo().observe(pantalla.getViewLifecycleOwner(), evento -> {
            YearMonth periodo = evento.tomar();
            if (periodo != null) {
                String mes = Formatos.periodo(periodo);
                new MaterialAlertDialogBuilder(pantalla.requireContext())
                        .setTitle(pantalla.getString(R.string.verif_mes_anterior_titulo, mes))
                        .setMessage(pantalla.getString(R.string.verif_mes_anterior_mensaje, mes))
                        .setPositiveButton(R.string.verif_mes_anterior_si, (d, w) -> guardar.accept(true))
                        .setNegativeButton(R.string.verif_mes_anterior_no, null)
                        .show();
            }
        });
        modelo.guardada().observe(pantalla.getViewLifecycleOwner(), evento -> {
            Long id = evento.tomar();
            if (id != null) {
                navegar(pantalla, destino, accionGuardada, id);
            }
        });
        pantalla.getChildFragmentManager().setFragmentResultListener(DuplicadoDialog.RESULTADO,
                pantalla.getViewLifecycleOwner(), (clave, resultado) -> {
                    if (DuplicadoDialog.VER.equals(resultado.getString(DuplicadoDialog.ACCION))) {
                        navegar(pantalla, destino, accionDetalle, resultado.getLong(DuplicadoDialog.ID_FACTURA));
                    } else {
                        ((App) pantalla.requireActivity().getApplication()).contenedor().capturaEnCurso().limpiar();
                        NavHostFragment.findNavController(pantalla).popBackStack(R.id.inicio, false);
                    }
                });
    }

    private static void navegar(Fragment pantalla, @IdRes int destino, @IdRes int accion, long idFactura) {
        NavController navegacion = NavHostFragment.findNavController(pantalla);
        if (navegacion.getCurrentDestination() == null || navegacion.getCurrentDestination().getId() != destino) {
            return;
        }
        Bundle argumentos = new Bundle();
        argumentos.putLong("idFactura", idFactura);
        navegacion.navigate(accion, argumentos);
    }
}
