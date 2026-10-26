package pe.facturass20.ui.ajustes;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pe.facturass20.R;
import pe.facturass20.databinding.DialogoCambiarPinBinding;
import pe.facturass20.databinding.FragmentAjustesBinding;
import pe.facturass20.databinding.ItemEnlaceBinding;
import pe.facturass20.databinding.ItemInterruptorBinding;
import pe.facturass20.dominio.modelo.ParametrosNrus;
import pe.facturass20.ui.comun.Filas;
import pe.facturass20.ui.comun.Formatos;
import pe.facturass20.ui.comun.Mensajes;

/**
 * P18 Ajustes (prototipo pantalla_18_ajustes_modelo_y.png): Mi negocio, Seguridad (PIN y huella), Captura
 * (volver a mostrar la guía de P05), Lector de facturas (→ P03), Parámetros del NRUS (→ P14) y Mis datos
 * (→ P20). El RUC y el nombre solo se muestran: cambiarlos cambiaría a quién pertenecen los registros.
 */
public class AjustesFragment extends Fragment {

    private FragmentAjustesBinding binding;
    private AjustesViewModel modelo;
    private ItemEnlaceBinding filaModelo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAjustesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(AjustesViewModel.class);

        Filas.enlace(binding.seccionSeguridad, getString(R.string.ajustes_cambiar_pin), null, this::cambiarPin);
        Filas.separador(binding.seccionSeguridad);
        ItemInterruptorBinding huella = Filas.interruptor(binding.seccionSeguridad, getString(R.string.p02_huella),
                modelo.huellaDisponible() ? null : getString(R.string.ajustes_huella_no_disponible),
                modelo.huellaActivada(), modelo::activarHuella);
        huella.interruptor.setEnabled(modelo.huellaDisponible());
        huella.getRoot().setEnabled(modelo.huellaDisponible());
        Filas.separador(binding.seccionSeguridad);
        Filas.interruptor(binding.seccionSeguridad, getString(R.string.ajustes_mostrar_guia),
                getString(R.string.ajustes_mostrar_guia_detalle), modelo.mostrarGuia(), modelo::mostrarGuia);

        filaModelo = Filas.enlace(binding.seccionLector, getString(R.string.ajustes_modelo),
                getString(R.string.ajustes_modelo_estado), () -> navegar(R.id.ajustes_a_preparacionModelo));
        Filas.separador(binding.seccionLector);
        Filas.enlace(binding.seccionLector, getString(R.string.ajustes_modelo_nuevo),
                getString(R.string.ajustes_modelo_nuevo_detalle), null);

        Filas.enlace(binding.seccionDatos, getString(R.string.ajustes_exportar), getString(R.string.historial_exportar_detalle),
                () -> navegar(R.id.ajustes_a_exportarDatos));

        binding.textoVersion.setText(getString(R.string.ajustes_version, versionApp()));
        modelo.vista().observe(getViewLifecycleOwner(), this::mostrar);
        Mensajes.observar(this, modelo, binding.getRoot());
        modelo.cargar();
    }

    private void mostrar(AjustesViewModel.Vista vista) {
        binding.desplazamiento.setVisibility(View.VISIBLE);
        binding.seccionNegocio.removeAllViews();
        Filas.enlace(binding.seccionNegocio, getString(R.string.detalle_ruc), vista.contribuyente().ruc(), null);
        Filas.separador(binding.seccionNegocio);
        Filas.enlace(binding.seccionNegocio, getString(R.string.ajustes_nombre), vista.contribuyente().nombre(), null);
        Filas.separador(binding.seccionNegocio);
        Filas.enlace(binding.seccionNegocio, getString(R.string.ajustes_titular), vista.contribuyente().titular(), null);

        filaModelo.textoDetalle.setText(vista.versionModelo() == null ? getString(R.string.ajustes_modelo_estado)
                : getString(R.string.ajustes_modelo_instalado, vista.versionModelo()));

        ParametrosNrus parametros = vista.parametros();
        binding.seccionParametros.removeAllViews();
        Filas.enlace(binding.seccionParametros, getString(R.string.ajustes_parametros_version),
                getString(R.string.ajustes_parametros_detalle, parametros.version(), Formatos.fecha(parametros.vigenteDesde())),
                () -> navegar(R.id.ajustes_a_categoria));
    }

    private void cambiarPin() {
        DialogoCambiarPinBinding dialogo = DialogoCambiarPinBinding.inflate(getLayoutInflater());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ajustes_cambiar_pin)
                .setView(dialogo.getRoot())
                .setPositiveButton(R.string.ajustes_guardar_pin, (d, w) -> modelo.cambiarPin(
                        leer(dialogo.campoActual.getEditText()), leer(dialogo.campoNuevo.getEditText()),
                        leer(dialogo.campoRepetido.getEditText())))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    /** El PIN como arreglo, para poder borrarlo después; el campo se vacía. */
    private static char[] leer(EditText campo) {
        Editable texto = campo.getText();
        char[] pin = new char[texto == null ? 0 : texto.length()];
        if (texto != null) {
            texto.getChars(0, texto.length(), pin, 0);
            texto.clear();
        }
        return pin;
    }

    private String versionApp() {
        try {
            return requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private void navegar(int accion) {
        NavHostFragment.findNavController(this).navigate(accion);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
