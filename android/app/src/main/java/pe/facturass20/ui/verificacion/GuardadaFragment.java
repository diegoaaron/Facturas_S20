package pe.facturass20.ui.verificacion;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentGuardadaBinding;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.NivelAlerta;
import pe.facturass20.ui.comun.Formatos;
import pe.facturass20.ui.comun.Mensajes;
import pe.facturass20.ui.inicio.ResumenInicio;

/**
 * P10 Guardada (prototipo pantalla_10_factura_guardada.png): importe agregado, compras del mes con la
 * barra del límite y el aviso si pasó el 80 % (RF-12). «Escanear otra factura» vuelve a P05; Atrás e
 * «Ir al inicio», a P04.
 */
public class GuardadaFragment extends Fragment {

    private FragmentGuardadaBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGuardadaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        GuardadaViewModel modelo = new ViewModelProvider(this).get(GuardadaViewModel.class);
        binding.botonEscanearOtra.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.guardada_a_guiaCaptura));
        binding.botonInicio.setOnClickListener(v -> irAlInicio());
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        irAlInicio();
                    }
                });

        modelo.vista().observe(getViewLifecycleOwner(), this::mostrar);
        Mensajes.observar(this, modelo, binding.getRoot());
        modelo.cargar(requireArguments().getLong("idFactura"));
    }

    private void mostrar(GuardadaViewModel.Vista vista) {
        FacturaCompra factura = vista.factura();
        ResumenInicio mes = vista.mes();
        binding.desplazamiento.setVisibility(View.VISIBLE);
        binding.textoDocumento.setText(getString(R.string.guardada_documento, factura.emisor().razonSocial(),
                factura.serie(), factura.numero()));
        binding.textoImporte.setText(getString(R.string.guardada_importe, Formatos.moneda(factura.importeTotal())));
        binding.textoComprasMes.setText(getString(R.string.guardada_compras_de, Formatos.mes(mes.periodo())));
        binding.textoCompras.setText(Formatos.moneda(mes.compras()));

        binding.barraLimite.setProgreso(mes.progreso());
        binding.barraLimite.setMarca(mes.fueraDeRegimen() ? -1 : mes.umbralAviso().floatValue());
        NivelAlerta nivel = mes.nivel();
        if (nivel == NivelAlerta.NINGUNA) {
            binding.barraLimite.setColorIndicador(color(R.color.color_acento));
            binding.textoAviso.setVisibility(View.GONE);
            return;
        }
        boolean grave = nivel != NivelAlerta.AVISO_80;
        binding.barraLimite.setColorIndicador(color(grave ? R.color.color_error : R.color.color_alerta));
        binding.textoAviso.setVisibility(View.VISIBLE);
        int colorTexto = color(grave ? R.color.color_error : R.color.color_alerta_oscuro);
        binding.textoAviso.setTextColor(colorTexto);
        TextViewCompat.setCompoundDrawableTintList(binding.textoAviso, ColorStateList.valueOf(colorTexto));
        if (nivel == NivelAlerta.AVISO_80) {
            binding.textoAviso.setText(getString(R.string.aviso_80, mes.categoria()));
        } else if (nivel == NivelAlerta.LIMITE_100) {
            binding.textoAviso.setText(getString(R.string.aviso_100, mes.categoria()));
        } else {
            binding.textoAviso.setText(getString(R.string.p04_fuera_limite, Formatos.monedaCorta(mes.limite())));
        }
    }

    private void irAlInicio() {
        NavHostFragment.findNavController(this).popBackStack(R.id.inicio, false);
    }

    private int color(@ColorRes int id) {
        return ContextCompat.getColor(requireContext(), id);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
