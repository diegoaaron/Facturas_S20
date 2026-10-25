package pe.facturass20.ui.facturas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentDetalleFacturaBinding;
import pe.facturass20.databinding.ItemDatoBinding;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.OrigenRegistro;
import pe.facturass20.ui.comun.Formatos;
import pe.facturass20.ui.comun.Mensajes;
import pe.facturass20.ui.comun.VisorImagen;

/**
 * P12 Detalle y anulación (prototipo pantalla_12_detalle_y_anulacion.png): la foto, los datos y cómo se
 * registró. «Anular» abre {@link AnulacionBottomSheet}; solo aparece si la factura está vigente y su mes
 * abierto. Compartir envía los datos como texto, nunca la foto.
 */
public class DetalleFacturaFragment extends Fragment {

    private FragmentDetalleFacturaBinding binding;
    private DetalleFacturaViewModel modelo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDetalleFacturaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(DetalleFacturaViewModel.class);
        binding.barra.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        getChildFragmentManager().setFragmentResultListener(AnulacionBottomSheet.RESULTADO, getViewLifecycleOwner(),
                (clave, resultado) -> modelo.anular(resultado.getString(AnulacionBottomSheet.MOTIVO)));

        modelo.vista().observe(getViewLifecycleOwner(), this::mostrar);
        modelo.ocupado().observe(getViewLifecycleOwner(), ocupado -> {
            binding.progreso.setVisibility(ocupado && modelo.vista().getValue() == null ? View.VISIBLE : View.GONE);
            binding.botonAnular.setEnabled(!ocupado);
        });
        Mensajes.observar(this, modelo, binding.getRoot());
        if (modelo.vista().getValue() == null) {
            modelo.cargar(requireArguments().getLong("idFactura"));
        }
    }

    private void mostrar(DetalleFacturaViewModel.Vista vista) {
        FacturaCompra factura = vista.factura();
        binding.desplazamiento.setVisibility(View.VISIBLE);

        if (vista.imagen() != null) {
            binding.imagen.setImageBitmap(vista.imagen());
            binding.imagen.setOnClickListener(v -> VisorImagen.mostrar(requireContext(), vista.imagen()));
            binding.textoSinFoto.setVisibility(View.GONE);
        } else {
            binding.imagen.setImageDrawable(null);
            binding.textoSinFoto.setVisibility(View.VISIBLE);
        }

        binding.listaDatos.removeAllViews();
        dato(R.string.campo_proveedor, factura.emisor().razonSocial());
        dato(R.string.detalle_ruc, factura.emisor().ruc());
        dato(R.string.detalle_serie_numero, factura.serie() + "-" + factura.numero());
        dato(R.string.campo_fecha, Formatos.fecha(factura.fechaEmision()));
        dato(R.string.campo_importe, Formatos.moneda(factura.importeTotal()));
        dato(R.string.detalle_registro, registro(vista));

        boolean vigente = factura.esVigente();
        binding.avisoAnulada.setVisibility(vigente ? View.GONE : View.VISIBLE);
        if (!vigente) {
            binding.textoAnulada.setText(getString(R.string.detalle_anulada, factura.motivoAnulacion()));
        }
        binding.textoMesCerrado.setVisibility(vigente && !vista.mesAbierto() ? View.VISIBLE : View.GONE);
        binding.botonAnular.setVisibility(vigente && vista.mesAbierto() ? View.VISIBLE : View.GONE);
        binding.botonAnular.setOnClickListener(v ->
                AnulacionBottomSheet.mostrar(this, Formatos.moneda(factura.importeTotal())));
        binding.barra.setOnMenuItemClickListener(item -> {
            compartir(factura);
            return true;
        });
    }

    private String registro(DetalleFacturaViewModel.Vista vista) {
        if (vista.factura().origen() == OrigenRegistro.MANUAL) {
            return getString(R.string.detalle_registro_manual);
        }
        if (vista.corregidos() == 0) {
            return getString(R.string.detalle_registro_ia);
        }
        return getResources().getQuantityString(R.plurals.detalle_registro_ia_corregida, vista.corregidos(),
                vista.corregidos());
    }

    private void dato(int etiqueta, String valor) {
        ItemDatoBinding fila = ItemDatoBinding.inflate(getLayoutInflater(), binding.listaDatos, true);
        fila.textoEtiqueta.setText(etiqueta);
        fila.textoValor.setText(valor);
    }

    private void compartir(FacturaCompra factura) {
        String texto = getString(R.string.detalle_compartir_texto, factura.emisor().razonSocial(),
                factura.emisor().ruc(), factura.serie(), factura.numero(), Formatos.fecha(factura.fechaEmision()),
                Formatos.moneda(factura.importeTotal()));
        Intent envio = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto);
        startActivity(Intent.createChooser(envio, getString(R.string.detalle_compartir)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
