package pe.facturass20.ui.acceso;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentConfiguracionBinding;
import pe.facturass20.dominio.reglas.ValidadorRuc;

/** P01 Configuración inicial. La lógica está en {@link ConfiguracionViewModel}. */
public class ConfiguracionFragment extends Fragment {

    private FragmentConfiguracionBinding binding;
    private ConfiguracionViewModel modelo;
    private OnBackPressedCallback atras;
    private AlertDialog dialogoHuella;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentConfiguracionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(ConfiguracionViewModel.class);

        binding.campoRuc.setEndIconVisible(false);
        binding.entradaRuc.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                mostrarEstadoRuc(s.toString());
            }
        });
        limpiarErrorAlEscribir(binding.campoNombre);
        limpiarErrorAlEscribir(binding.campoTitular);
        binding.entradaTitular.setOnEditorActionListener((v, accion, evento) -> {
            continuar();
            return true;
        });
        binding.botonContinuar.setOnClickListener(v -> continuar());
        binding.teclado.setAlCompletar(modelo::ingresarPin);
        binding.botonVolver.setOnClickListener(v -> modelo.volver());

        atras = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                modelo.volver();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), atras);

        modelo.paso().observe(getViewLifecycleOwner(), this::mostrarPaso);
        modelo.errores().observe(getViewLifecycleOwner(), this::mostrarErrores);
        modelo.avisoPin().observe(getViewLifecycleOwner(), evento -> {
            Integer aviso = evento.tomar();
            if (aviso != null) {
                binding.textoAvisoPin.setText(aviso);
                binding.textoAvisoPin.setVisibility(View.VISIBLE);
                binding.teclado.sacudir();
            }
        });
        modelo.ocupado().observe(getViewLifecycleOwner(), ocupado -> binding.botonVolver.setEnabled(!ocupado));
        modelo.mensaje().observe(getViewLifecycleOwner(), evento -> {
            String texto = evento.tomar();
            if (texto != null) {
                Snackbar.make(binding.getRoot(), texto, Snackbar.LENGTH_LONG).show();
            }
        });
        modelo.ofrecerHuella().observe(getViewLifecycleOwner(), ofrecer -> {
            if (ofrecer && dialogoHuella == null) {
                ofrecerHuella();
            }
        });
    }

    private void continuar() {
        modelo.continuar(texto(binding.entradaRuc.getText()), texto(binding.entradaNombre.getText()),
                texto(binding.entradaTitular.getText()));
    }

    private void mostrarPaso(int paso) {
        boolean negocio = paso == ConfiguracionViewModel.PASO_NEGOCIO;
        binding.grupoNegocio.setVisibility(negocio ? View.VISIBLE : View.GONE);
        binding.grupoPin.setVisibility(negocio ? View.GONE : View.VISIBLE);
        atras.setEnabled(!negocio);
        if (negocio) {
            binding.textoTitulo.setText(R.string.p01_titulo_negocio);
            binding.textoPaso.setText(R.string.p01_paso1);
        } else {
            boolean repetir = paso == ConfiguracionViewModel.PASO_REPETIR_PIN;
            binding.textoTitulo.setText(repetir ? R.string.p01_titulo_repetir_pin : R.string.p01_titulo_pin);
            binding.textoPaso.setText(repetir ? R.string.p01_paso3 : R.string.p01_paso2);
            if (repetir) {
                binding.textoAvisoPin.setVisibility(View.GONE);
            }
            binding.teclado.limpiar();
        }
        pintarIndicador(binding.indicadorPaso1, paso >= 1);
        pintarIndicador(binding.indicadorPaso2, paso >= 2);
        pintarIndicador(binding.indicadorPaso3, paso >= 3);
    }

    private void pintarIndicador(View indicador, boolean activo) {
        indicador.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                activo ? R.color.color_acento : R.color.color_borde)));
    }

    private void mostrarErrores(ConfiguracionViewModel.ErroresNegocio errores) {
        if (errores.ruc() != 0) {
            mostrarError(binding.campoRuc, errores.ruc());
        }
        mostrarError(binding.campoNombre, errores.nombre());
        mostrarError(binding.campoTitular, errores.titular());
    }

    /** Check verde y «RUC válido» con 11 dígitos correctos; error si los 11 no pasan el módulo 11. */
    private void mostrarEstadoRuc(String ruc) {
        boolean valido = ValidadorRuc.esValido(ruc);
        TextInputLayout campo = binding.campoRuc;
        campo.setEndIconVisible(valido);
        int error = ConfiguracionViewModel.errorRucAlEscribir(ruc);
        if (error != 0) {
            mostrarError(campo, error);
        } else {
            campo.setError(null);
            campo.setHelperText(valido ? getString(R.string.p01_ruc_valido) : null);
        }
        if (valido) {
            campo.setBoxStrokeColorStateList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                    R.color.color_ok)));
        } else {
            campo.setBoxStrokeColorStateList(ContextCompat.getColorStateList(requireContext(), R.color.campo_borde));
        }
    }

    private void mostrarError(TextInputLayout campo, @StringRes int error) {
        campo.setError(error == 0 ? null : getString(error));
    }

    private static void limpiarErrorAlEscribir(TextInputLayout campo) {
        if (campo.getEditText() == null) {
            return;
        }
        campo.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                campo.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void ofrecerHuella() {
        dialogoHuella = new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_huella)
                .setTitle(R.string.p01_huella_titulo)
                .setMessage(R.string.p01_huella_mensaje)
                .setPositiveButton(R.string.p01_huella_si, (dialogo, boton) -> modelo.terminar(true))
                .setNegativeButton(R.string.p01_huella_no, (dialogo, boton) -> modelo.terminar(false))
                .setCancelable(false)
                .show();
    }

    private static String texto(Editable editable) {
        return editable == null ? "" : editable.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dialogoHuella != null) {
            dialogoHuella.dismiss();
            dialogoHuella = null;
        }
        binding = null;
    }
}
