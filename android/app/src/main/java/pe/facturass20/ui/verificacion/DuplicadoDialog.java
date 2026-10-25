package pe.facturass20.ui.verificacion;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pe.facturass20.R;
import pe.facturass20.databinding.DialogoDuplicadoBinding;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.ui.comun.Formatos;

/**
 * P09 Duplicado (prototipo pantalla_09_aviso_de_duplicado.png, RF-07): la factura ya está registrada con
 * el mismo RUC, serie y número. Lo abren P08 y P19 como diálogo hijo; la elección vuelve con
 * {@link #RESULTADO}: {@link #VER} (abrir la registrada) o {@link #DESCARTAR}.
 */
public class DuplicadoDialog extends DialogFragment {

    public static final String RESULTADO = "duplicado";
    public static final String ACCION = "accion";
    public static final String ID_FACTURA = "idFactura";
    public static final String VER = "ver";
    public static final String DESCARTAR = "descartar";

    private static final String PROVEEDOR = "proveedor";
    private static final String DOCUMENTO = "documento";
    private static final String DETALLE = "detalle";
    private static final String TEXTO_DESCARTAR = "textoDescartar";

    /** @param textoDescartar «Descartar esta foto» (P08) o «Descartar este registro» (P19) */
    public static void mostrar(Fragment padre, FacturaCompra existente, @StringRes int textoDescartar) {
        Bundle argumentos = new Bundle();
        argumentos.putLong(ID_FACTURA, existente.id() == null ? 0 : existente.id());
        argumentos.putString(PROVEEDOR, existente.emisor().razonSocial());
        argumentos.putString(DOCUMENTO, existente.serie() + "-" + existente.numero());
        argumentos.putString(DETALLE, padre.getString(R.string.duplicado_detalle,
                Formatos.fecha(existente.fechaEmision()), Formatos.moneda(existente.importeTotal())));
        argumentos.putInt(TEXTO_DESCARTAR, textoDescartar);
        DuplicadoDialog dialogo = new DuplicadoDialog();
        dialogo.setArguments(argumentos);
        dialogo.show(padre.getChildFragmentManager(), RESULTADO);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle argumentos = requireArguments();
        DialogoDuplicadoBinding binding = DialogoDuplicadoBinding.inflate(getLayoutInflater());
        binding.textoDocumento.setText(getString(R.string.duplicado_documento, argumentos.getString(DOCUMENTO),
                argumentos.getString(PROVEEDOR)));
        binding.textoDetalle.setText(argumentos.getString(DETALLE));
        binding.botonDescartar.setText(argumentos.getInt(TEXTO_DESCARTAR));
        binding.botonVer.setOnClickListener(v -> responder(VER));
        binding.botonDescartar.setOnClickListener(v -> responder(DESCARTAR));
        AlertDialog dialogo = new MaterialAlertDialogBuilder(requireContext()).setView(binding.getRoot()).create();
        setCancelable(false);
        return dialogo;
    }

    private void responder(String accion) {
        Bundle resultado = new Bundle();
        resultado.putString(ACCION, accion);
        resultado.putLong(ID_FACTURA, requireArguments().getLong(ID_FACTURA));
        getParentFragmentManager().setFragmentResult(RESULTADO, resultado);
        dismiss();
    }
}
