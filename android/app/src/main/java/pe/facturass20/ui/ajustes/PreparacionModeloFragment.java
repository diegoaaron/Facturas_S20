package pe.facturass20.ui.ajustes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import java.util.Locale;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentPreparacionModeloBinding;
import pe.facturass20.databinding.ItemComprobacionBinding;
import pe.facturass20.ui.comun.Mensajes;

/**
 * P03 Preparar lectura con IA (prototipo pantalla_03_preparar_lectura_con.png): memoria, espacio,
 * conexión y versión de Android del teléfono, y el estado del modelo. «Descargar» queda desactivado hasta
 * que el modelo convertido esté publicado (I4); «Registrar a mano» abre P19.
 */
public class PreparacionModeloFragment extends Fragment {

    private FragmentPreparacionModeloBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPreparacionModeloBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        PreparacionModeloViewModel modelo = new ViewModelProvider(this).get(PreparacionModeloViewModel.class);
        binding.barra.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.textoDescarga.setText(getString(R.string.modelo_descarga, gigas(PreparacionModeloViewModel.TAMANO_MODELO)));
        binding.botonManual.setOnClickListener(v -> {
            PreparacionModeloViewModel.Equipo equipo = modelo.equipo().getValue();
            Bundle argumentos = new Bundle();
            argumentos.putString("motivo", equipo != null && !equipo.apto() ? "sin_ia" : "guia");
            NavHostFragment.findNavController(this).navigate(R.id.preparacionModelo_a_registroManual, argumentos);
        });
        modelo.equipo().observe(getViewLifecycleOwner(), this::mostrar);
        Mensajes.observar(this, modelo, binding.getRoot());
        modelo.comprobar();
    }

    private void mostrar(PreparacionModeloViewModel.Equipo equipo) {
        binding.comprobaciones.removeAllViews();
        comprobacion(R.string.modelo_memoria, gigas(equipo.memoria()), equipo.memoriaSuficiente());
        comprobacion(R.string.modelo_espacio, getString(R.string.modelo_espacio_valor, gigas(equipo.espacioLibre()),
                gigas(PreparacionModeloViewModel.ESPACIO_MINIMO)), equipo.espacioSuficiente());
        comprobacion(R.string.modelo_conexion, getString(equipo.wifi() ? R.string.modelo_wifi : R.string.modelo_sin_wifi),
                equipo.wifi());
        comprobacion(R.string.modelo_android, equipo.android(), true);
        if (!equipo.apto()) {
            binding.textoEstado.setText(R.string.modelo_no_apto);
        }
    }

    private void comprobacion(int etiqueta, String valor, boolean bien) {
        ItemComprobacionBinding fila = ItemComprobacionBinding.inflate(getLayoutInflater(), binding.comprobaciones, true);
        fila.icono.setImageResource(bien ? R.drawable.ic_check_circulo : R.drawable.ic_error_circulo);
        fila.icono.setColorFilter(ContextCompat.getColor(requireContext(), bien ? R.color.color_ok : R.color.color_error));
        fila.textoEtiqueta.setText(etiqueta);
        fila.textoValor.setText(valor);
        fila.getRoot().setContentDescription(getString(bien ? R.string.modelo_cumple : R.string.modelo_no_cumple,
                getString(etiqueta), valor));
    }

    /** {@code 2,6 GB}, con coma decimal como el resto de la app. */
    private static String gigas(long bytes) {
        double gb = bytes / (1024.0 * 1024 * 1024);
        return String.format(Locale.ROOT, "%.1f", gb).replace('.', ',') + " GB";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
