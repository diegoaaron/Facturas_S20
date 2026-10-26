package pe.facturass20.ui.verificacion;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentLecturaBinding;
import pe.facturass20.databinding.ItemPasoLecturaBinding;
import pe.facturass20.ui.comun.Formatos;

/**
 * P07 Lectura (prototipo pantalla_07_lectura_en_el.png): muestra el avance mientras la IA lee la foto y
 * pasa a P08 cuando termina. Si falla o se pasa del límite, ofrece tomar otra foto o registrar a mano.
 */
public class LecturaFragment extends Fragment {

    /** Grupos de campos que se marcan a medida que avanza la lectura, como en el prototipo. */
    private static final int[] PASOS = {R.string.lectura_paso_proveedor, R.string.lectura_paso_serie,
            R.string.lectura_paso_fecha, R.string.lectura_paso_moneda, R.string.lectura_paso_importe};

    private FragmentLecturaBinding binding;
    private LecturaViewModel modelo;
    private final List<ItemPasoLecturaBinding> pasos = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(LecturaViewModel.class);

        pasos.clear();
        for (int paso : PASOS) {
            ItemPasoLecturaBinding fila = ItemPasoLecturaBinding.inflate(getLayoutInflater(), binding.listaCampos, true);
            fila.texto.setText(paso);
            pasos.add(fila);
        }

        binding.barra.setNavigationOnClickListener(v -> cancelar());
        binding.botonCancelar.setOnClickListener(v -> cancelar());
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        cancelar();
                    }
                });

        modelo.progreso().observe(getViewLifecycleOwner(), this::mostrarProgreso);
        modelo.segundosRestantes().observe(getViewLifecycleOwner(), segundos -> binding.textoRestante.setText(
                segundos > 0 ? getString(R.string.lectura_restante, segundos) : getString(R.string.lectura_casi_listo)));
        modelo.versionModelo().observe(getViewLifecycleOwner(), version -> binding.textoLector.setText(
                version != null ? getString(R.string.lectura_modelo_gemma, version) : getString(R.string.lectura_modelo_prueba)));
        modelo.estado().observe(getViewLifecycleOwner(), estado -> {
            if (estado == LecturaViewModel.Estado.LISTA) {
                navegar(R.id.lectura_a_verificacion, null);
            } else if (estado == LecturaViewModel.Estado.FALLO) {
                mostrarFallo(modelo.error());
            }
        });
        modelo.iniciar();
    }

    private void mostrarProgreso(int porcentaje) {
        binding.circulo.setProgressCompat(porcentaje, true);
        binding.textoPorcentaje.setText(porcentaje + String.valueOf(Formatos.ESPACIO) + "%");
        for (int i = 0; i < pasos.size(); i++) {
            boolean listo = porcentaje == 100 || porcentaje >= (i + 1) * 90 / pasos.size();
            ItemPasoLecturaBinding fila = pasos.get(i);
            fila.icono.setImageResource(listo ? R.drawable.ic_check_circulo : R.drawable.ic_circulo_vacio);
            fila.icono.setColorFilter(ContextCompat.getColor(requireContext(), listo ? R.color.color_ok : R.color.color_borde));
            fila.texto.setTextColor(ContextCompat.getColor(requireContext(), listo ? R.color.color_texto : R.color.color_texto2));
            fila.texto.setTypeface(null, listo ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private void mostrarFallo(String motivo) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.lectura_fallo_titulo)
                .setMessage(motivo)
                .setCancelable(false)
                .setPositiveButton(R.string.lectura_otra_foto, (d, w) -> otraFoto())
                .setNegativeButton(R.string.lectura_a_mano, (d, w) -> {
                    Bundle argumentos = new Bundle();
                    argumentos.putString("motivo", "lectura");
                    navegar(R.id.lectura_a_registroManual, argumentos);
                })
                .show();
    }

    /** Como en P08: vuelve a P05 si sigue en la pila, si no la abre de nuevo. */
    private void otraFoto() {
        NavController navegacion = NavHostFragment.findNavController(this);
        if (!navegacion.popBackStack(R.id.guiaCaptura, false)) {
            navegar(R.id.accion_escanear, null, new NavOptions.Builder().setPopUpTo(R.id.lectura, true).build());
        }
    }

    private void cancelar() {
        modelo.cancelar();
        NavHostFragment.findNavController(this).navigateUp();
    }

    private void navegar(int accion, Bundle argumentos) {
        navegar(accion, argumentos, null);
    }

    private void navegar(int accion, Bundle argumentos, NavOptions opciones) {
        NavController navegacion = NavHostFragment.findNavController(this);
        if (navegacion.getCurrentDestination() != null && navegacion.getCurrentDestination().getId() == R.id.lectura) {
            navegacion.navigate(accion, argumentos, opciones);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pasos.clear();
        binding = null;
    }
}
