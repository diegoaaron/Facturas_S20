package pe.facturass20.ui.acceso;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import pe.facturass20.R;
import pe.facturass20.databinding.DialogoRestablecerPinBinding;
import pe.facturass20.databinding.FragmentAccesoBinding;

/** P02 Acceso. La lógica está en {@link AccesoViewModel}. */
public class AccesoFragment extends Fragment {

    private FragmentAccesoBinding binding;
    private AccesoViewModel modelo;
    private BiometricPrompt huella;
    private OnBackPressedCallback atras;
    private AlertDialog dialogo;
    private final Runnable cuentaAtras = this::actualizarCuentaAtras;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAccesoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(AccesoViewModel.class);
        huella = new BiometricPrompt(this, ContextCompat.getMainExecutor(requireContext()),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult resultado) {
                        modelo.desbloquearConHuella();
                    }
                });

        binding.teclado.setAlCompletar(modelo::ingresarPin);
        binding.botonHuella.setOnClickListener(v -> pedirHuella());
        atras = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                modelo.cancelarRestablecer();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), atras);

        modelo.nombreNegocio().observe(getViewLifecycleOwner(),
                nombre -> binding.textoSaludo.setText(getString(R.string.p02_saludo, nombre)));
        modelo.modo().observe(getViewLifecycleOwner(), this::mostrarModo);
        modelo.aviso().observe(getViewLifecycleOwner(), binding.textoAviso::setText);
        modelo.esperaHasta().observe(getViewLifecycleOwner(), hasta -> actualizarCuentaAtras());
        modelo.ocupado().observe(getViewLifecycleOwner(), ocupado -> binding.botonRestablecer.setEnabled(!ocupado));
        modelo.pinRechazado().observe(getViewLifecycleOwner(), evento -> {
            if (evento.tomar() != null) {
                binding.teclado.sacudir();
                binding.teclado.limpiar();
                actualizarCuentaAtras();
            }
        });
        modelo.mensaje().observe(getViewLifecycleOwner(), evento -> {
            String texto = evento.tomar();
            if (texto != null) {
                Snackbar.make(binding.getRoot(), texto, Snackbar.LENGTH_LONG).show();
            }
        });

        // Al abrir la app, la huella se pide sola; con «Usar PIN» queda el teclado.
        if (savedInstanceState == null && modelo.huellaActivada()) {
            binding.getRoot().post(this::pedirHuella);
        }
    }

    private void mostrarModo(AccesoViewModel.Modo modo) {
        boolean ingresar = modo == AccesoViewModel.Modo.INGRESAR;
        atras.setEnabled(!ingresar);
        binding.botonHuella.setVisibility(ingresar && modelo.huellaActivada() ? View.VISIBLE : View.GONE);
        binding.botonRestablecer.setText(ingresar ? R.string.p02_olvido : R.string.p02_cancelar_restablecer);
        binding.botonRestablecer.setOnClickListener(ingresar ? v -> pedirRuc() : v -> modelo.cancelarRestablecer());
        if (ingresar) {
            binding.textoInstruccion.setText(R.string.p02_instruccion);
        } else {
            binding.textoInstruccion.setText(modo == AccesoViewModel.Modo.NUEVO_PIN
                    ? R.string.p02_nuevo_pin : R.string.p02_repetir_pin);
        }
        binding.teclado.limpiar();
        actualizarCuentaAtras();
    }

    /** Muestra los segundos que faltan y deshabilita el teclado hasta que termine la espera. */
    private void actualizarCuentaAtras() {
        if (binding == null) {
            return;
        }
        binding.getRoot().removeCallbacks(cuentaAtras);
        Long hasta = modelo.esperaHasta().getValue();
        long restantes = hasta == null || hasta == 0L ? 0 : hasta - SystemClock.elapsedRealtime();
        if (restantes > 0) {
            int segundos = (int) ((restantes + 999) / 1000);
            binding.textoAviso.setText(getResources().getQuantityString(R.plurals.p02_espera_segundos,
                    segundos, segundos));
            binding.teclado.setHabilitado(false);
            binding.getRoot().postDelayed(cuentaAtras, Math.min(restantes, 1000));
        } else {
            if (hasta != null && hasta != 0L) {
                modelo.comprobarEspera();
            }
            binding.teclado.setHabilitado(true);
        }
    }

    private void pedirHuella() {
        if (binding == null || !modelo.huellaActivada()) {
            return;
        }
        BiometricPrompt.PromptInfo datos = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.p02_huella))
                .setSubtitle(getString(R.string.app_name))
                .setNegativeButtonText(getString(R.string.p02_usar_pin))
                .setAllowedAuthenticators(PreferenciasAcceso.AUTENTICADORES)
                .build();
        huella.authenticate(datos);
    }

    private void pedirRuc() {
        DialogoRestablecerPinBinding vista = DialogoRestablecerPinBinding.inflate(getLayoutInflater());
        dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.p02_restablecer_titulo)
                .setMessage(R.string.p02_restablecer_mensaje)
                .setView(vista.getRoot())
                .setPositiveButton(R.string.p02_restablecer_continuar, (d, boton) -> {
                    CharSequence ruc = vista.entradaRuc.getText();
                    modelo.restablecer(ruc == null ? "" : ruc.toString());
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.getRoot().removeCallbacks(cuentaAtras);
        if (dialogo != null) {
            dialogo.dismiss();
            dialogo = null;
        }
        binding = null;
    }
}
