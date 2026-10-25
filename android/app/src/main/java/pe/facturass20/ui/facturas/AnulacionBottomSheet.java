package pe.facturass20.ui.facturas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import pe.facturass20.R;
import pe.facturass20.databinding.HojaAnulacionBinding;

/**
 * Hoja «Anular esta factura» de P12 (RF-18): motivo y confirmación. El motivo vuelve en
 * {@link #RESULTADO}; la anulación la hace {@link DetalleFacturaViewModel}.
 */
public class AnulacionBottomSheet extends BottomSheetDialogFragment {

    public static final String RESULTADO = "anulacion";
    public static final String MOTIVO = "motivo";
    private static final String IMPORTE = "importe";

    private HojaAnulacionBinding binding;

    /** @param importe ya formateado, p. ej. «S/ 386,40» */
    public static void mostrar(Fragment padre, String importe) {
        Bundle argumentos = new Bundle();
        argumentos.putString(IMPORTE, importe);
        AnulacionBottomSheet hoja = new AnulacionBottomSheet();
        hoja.setArguments(argumentos);
        hoja.show(padre.getChildFragmentManager(), RESULTADO);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = HojaAnulacionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.textoDescuento.setText(getString(R.string.anulacion_descuento, requireArguments().getString(IMPORTE)));
        binding.motivos.setOnCheckedChangeListener((grupo, id) ->
                binding.campoOtro.setVisibility(id == R.id.motivo_otro ? View.VISIBLE : View.GONE));
        binding.botonCancelar.setOnClickListener(v -> dismiss());
        binding.botonAnular.setOnClickListener(v -> confirmar());
    }

    private void confirmar() {
        String motivo;
        int elegido = binding.motivos.getCheckedRadioButtonId();
        if (elegido == R.id.motivo_otro) {
            CharSequence escrito = binding.campoOtro.getEditText().getText();
            motivo = escrito == null ? "" : escrito.toString().trim();
            if (motivo.isEmpty()) {
                binding.campoOtro.setError(getString(R.string.anulacion_otro_falta));
                return;
            }
        } else if (elegido == R.id.motivo_duplicado) {
            motivo = getString(R.string.anulacion_motivo_duplicado);
        } else if (elegido == R.id.motivo_devolucion) {
            motivo = getString(R.string.anulacion_motivo_devolucion);
        } else {
            motivo = getString(R.string.anulacion_motivo_error);
        }
        Bundle resultado = new Bundle();
        resultado.putString(MOTIVO, motivo);
        getParentFragmentManager().setFragmentResult(RESULTADO, resultado);
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
