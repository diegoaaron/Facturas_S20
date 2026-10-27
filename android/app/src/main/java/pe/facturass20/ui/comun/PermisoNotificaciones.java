package pe.facturass20.ui.comun;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import pe.facturass20.App;
import pe.facturass20.R;
import pe.facturass20.trabajos.RecordatorioWorker;
import pe.facturass20.ui.mes.PreferenciasAvisos;

/**
 * El permiso de notificaciones de Android 13+ (POST_NOTIFICATIONS), para los avisos del NRUS. P04 lo pide
 * una sola vez; P14 y P15, al activar un aviso. Se crea como campo del Fragment (registra el lanzador).
 */
public final class PermisoNotificaciones {

    private final Fragment fragmento;
    private final ActivityResultLauncher<String> lanzador;
    /** Si al negarlo hay que explicar que los avisos no llegarán (cuando el usuario activó uno). */
    private boolean explicarSiNiega;

    public PermisoNotificaciones(Fragment fragmento) {
        this.fragmento = fragmento;
        lanzador = fragmento.registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
            Context contexto = fragmento.getContext();
            if (contexto == null) {
                return;
            }
            if (concedido) {
                // Lo que tocaba avisar mientras no había permiso llega ahora.
                RecordatorioWorker.revisarAhora(contexto);
            } else if (explicarSiNiega) {
                Toast.makeText(contexto, R.string.permiso_notificaciones_negado, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static boolean concedido(Context contexto) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** P04: la primera vez que se entra, porque los avisos vienen activados. */
    public void pedirUnaVez() {
        Context contexto = fragmento.requireContext();
        PreferenciasAvisos preferencias = ((App) contexto.getApplicationContext()).contenedor().preferenciasAvisos();
        if (concedido(contexto) || preferencias.permisoPedido()) {
            return;
        }
        preferencias.marcarPermisoPedido();
        explicarSiNiega = false;
        lanzador.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    /** Al activar un aviso en P14 o P15. Si Android ya no deja preguntar, explica que no llegará. */
    public void pedirAlActivar() {
        if (concedido(fragmento.requireContext())) {
            return;
        }
        explicarSiNiega = true;
        lanzador.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
}
