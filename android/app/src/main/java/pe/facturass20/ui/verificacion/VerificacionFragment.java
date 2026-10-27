package pe.facturass20.ui.verificacion;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentVerificacionBinding;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.ui.comun.Mensajes;
import pe.facturass20.ui.comun.VisorImagen;

/**
 * P08 Verificación (prototipo pantalla_08_verificacion_de_datos.png): la foto con lupa, los 7 campos en
 * verde, ámbar o rojo, «Guardar factura» y «Tomar otra foto». Guardar sigue a P10, o abre P09 si la
 * factura ya está registrada.
 */
public class VerificacionFragment extends Fragment {

    private FragmentVerificacionBinding binding;
    private VerificacionViewModel modelo;
    private CamposFactura campos;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVerificacionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(VerificacionViewModel.class);
        binding.barra.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        if (!modelo.iniciar()) {
            Snackbar.make(binding.getRoot(), R.string.verif_sin_foto, Snackbar.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).popBackStack(R.id.inicio, false);
            return;
        }

        campos = new CamposFactura(binding.campos);
        if (savedInstanceState == null) {
            for (CampoFactura campo : CampoFactura.values()) {
                if (campo == CampoFactura.MONEDA) {
                    continue;
                }
                campos.poner(campo, modelo.textoLeido(campo));
            }
        }
        for (CampoFactura campo : CampoFactura.values()) {
            campos.pintar(campo, modelo.revisar(campo, campos.texto(campo)));
        }
        campos.alCambiar((campo, texto) -> campos.pintar(campo, modelo.revisar(campo, texto)));
        binding.textoCamposLeidos.setText(getResources().getQuantityString(R.plurals.verif_campos_leidos,
                modelo.camposLeidos(), modelo.camposLeidos()));

        modelo.imagen().observe(getViewLifecycleOwner(), imagen -> {
            binding.imagen.setImageBitmap(imagen);
            binding.botonAmpliar.setOnClickListener(v -> VisorImagen.mostrar(requireContext(), imagen));
            binding.imagen.setOnClickListener(v -> VisorImagen.mostrar(requireContext(), imagen));
        });
        binding.botonGuardar.setOnClickListener(v -> guardar(false));
        binding.botonOtraFoto.setOnClickListener(v -> otraFoto());

        FlujoGuardado.conectar(this, modelo, campos, R.id.verificacion, R.id.verificacion_a_guardada,
                R.id.verificacion_a_detalleFactura, R.string.duplicado_descartar_foto, this::guardar);
        modelo.ocupado().observe(getViewLifecycleOwner(), ocupado -> {
            binding.progreso.setVisibility(ocupado ? View.VISIBLE : View.GONE);
            binding.botonGuardar.setText(ocupado ? "" : getString(R.string.verif_guardar));
            binding.botonGuardar.setEnabled(!ocupado);
            binding.botonOtraFoto.setEnabled(!ocupado);
        });
        Mensajes.observar(this, modelo, binding.getRoot());
    }

    private void guardar(boolean periodoConfirmado) {
        campos.tocarTodos();
        modelo.guardar(campos.textos(), periodoConfirmado);
    }

    /** Vuelve a P05 si sigue en la pila; si no (la guía está oculta), la abre de nuevo y P05 abre la cámara sola. */
    private void otraFoto() {
        NavController navegacion = NavHostFragment.findNavController(this);
        if (!navegacion.popBackStack(R.id.guiaCaptura, false)) {
            navegacion.navigate(R.id.accion_escanear, null,
                    new NavOptions.Builder().setPopUpTo(R.id.verificacion, true).build());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
