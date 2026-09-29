package pe.facturass20.ui.comun;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentPantallaPendienteBinding;

/**
 * Pantalla provisional de los destinos del grafo de navegación que todavía no se construyen.
 * Muestra el código (P01–P20) y el nombre que recibe en los argumentos {@code codigo} y {@code titulo}.
 */
public class PantallaPendienteFragment extends Fragment {

    private FragmentPantallaPendienteBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPantallaPendienteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle argumentos = requireArguments();
        binding.textoCodigo.setText(getString(R.string.pendiente_codigo, argumentos.getString("codigo")));
        binding.textoTitulo.setText(argumentos.getInt("titulo"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
