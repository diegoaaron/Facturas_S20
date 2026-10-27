package pe.facturass20.ui.mes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentVentasBinding;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.ui.comun.Formatos;
import pe.facturass20.ui.comun.Mensajes;

/**
 * P13 Ventas del mes (prototipo pantalla_13_ventas_del_mes.png, RF-09): el total de ventas con un teclado
 * de montos y las compras como referencia. Al guardar sigue a P14 para ver qué categoría quedó; si se abrió
 * desde P17 para un mes anterior (argumento {@code periodo}), vuelve a P17. Si el mes ya está cerrado, solo
 * se muestra.
 */
public class VentasFragment extends Fragment {

    private FragmentVentasBinding binding;
    private VentasViewModel modelo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVentasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(VentasViewModel.class);
        binding.barra.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        View[] digitos = {binding.tecla0, binding.tecla1, binding.tecla2, binding.tecla3, binding.tecla4,
                binding.tecla5, binding.tecla6, binding.tecla7, binding.tecla8, binding.tecla9};
        for (int i = 0; i < digitos.length; i++) {
            char digito = (char) ('0' + i);
            digitos[i].setOnClickListener(v -> modelo.digito(digito));
        }
        binding.teclaPunto.setOnClickListener(v -> modelo.punto());
        binding.teclaBorrar.setOnClickListener(v -> modelo.borrar());
        binding.teclaBorrar.setOnLongClickListener(v -> {
            for (int i = 0; i < 12; i++) {
                modelo.borrar();
            }
            return true;
        });
        binding.botonGuardar.setOnClickListener(v -> modelo.guardar());

        modelo.texto().observe(getViewLifecycleOwner(), texto -> {
            boolean vacio = texto.isEmpty();
            binding.textoMonto.setText(vacio ? Formatos.moneda(java.math.BigDecimal.ZERO)
                    : "S/" + Formatos.ESPACIO + texto);
            binding.textoMonto.setTextColor(ContextCompat.getColor(requireContext(),
                    vacio ? R.color.color_inactivo : R.color.color_primario));
        });
        modelo.periodo().observe(getViewLifecycleOwner(), this::mostrar);
        modelo.ocupado().observe(getViewLifecycleOwner(), ocupado -> binding.botonGuardar.setEnabled(!ocupado
                && modelo.periodo().getValue() != null && modelo.periodo().getValue().estaAbierto()));
        modelo.guardadas().observe(getViewLifecycleOwner(), evento -> {
            if (evento.tomar() != null) {
                NavController navegacion = NavHostFragment.findNavController(this);
                if (navegacion.getCurrentDestination() == null
                        || navegacion.getCurrentDestination().getId() != R.id.ventas) {
                    return;
                }
                if (periodoArgumento().isEmpty()) {
                    navegacion.navigate(R.id.ventas_a_categoria);
                } else {
                    navegacion.navigateUp();
                }
            }
        });
        Mensajes.observar(this, modelo, binding.getRoot());
        modelo.cargar(periodoArgumento());
    }

    private String periodoArgumento() {
        return getArguments() == null ? "" : getArguments().getString("periodo", "");
    }

    private void mostrar(PeriodoMensual periodo) {
        binding.barra.setTitle(getString(R.string.ventas_titulo, Formatos.mes(periodo.periodo())));
        binding.textoCompras.setText(getString(R.string.ventas_compras, Formatos.moneda(periodo.totalAdquisiciones())));
        boolean abierto = periodo.estaAbierto();
        binding.botonGuardar.setEnabled(abierto);
        binding.teclado.setVisibility(abierto ? View.VISIBLE : View.GONE);
        binding.textoAyuda.setText(abierto ? R.string.ventas_hasta_cerrar : R.string.ventas_mes_cerrado);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
