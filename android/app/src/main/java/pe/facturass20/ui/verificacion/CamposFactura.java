package pe.facturass20.ui.verificacion;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import pe.facturass20.R;
import pe.facturass20.databinding.CamposFacturaBinding;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.Moneda;

/**
 * Los campos de {@code campos_factura.xml} de P08 y P19: los lee, los escribe y los pinta según
 * {@link RevisionCampo}. Un campo vacío o inválido solo se pinta de rojo después de que el usuario lo
 * tocó o intentó guardar, para no recibirlo con errores en el registro manual.
 *
 * <p>La moneda no se muestra: la app solo registra compras en soles, así que siempre vale {@code PEN}.</p>
 */
public final class CamposFactura {

    private final Context contexto;
    private final Map<CampoFactura, TextInputLayout> campos = new EnumMap<>(CampoFactura.class);
    private final Set<CampoFactura> tocados = EnumSet.noneOf(CampoFactura.class);
    private boolean escribiendo;

    public CamposFactura(CamposFacturaBinding binding) {
        contexto = binding.getRoot().getContext();
        campos.put(CampoFactura.RUC, binding.campoRuc);
        campos.put(CampoFactura.RAZON_SOCIAL, binding.campoRazonSocial);
        campos.put(CampoFactura.SERIE, binding.campoSerie);
        campos.put(CampoFactura.NUMERO, binding.campoNumero);
        campos.put(CampoFactura.FECHA_EMISION, binding.campoFecha);
        campos.put(CampoFactura.MONEDA, binding.campoMoneda);
        campos.put(CampoFactura.IMPORTE_TOTAL, binding.campoImporteTotal);
        binding.campoMoneda.setVisibility(View.GONE);
        poner(CampoFactura.MONEDA, Moneda.PEN.name());
    }

    public TextInputLayout campo(CampoFactura campo) {
        return campos.get(campo);
    }

    /** Avisa cada vez que el usuario cambia un campo (no cuando lo cambia el código con {@link #poner}). */
    public void alCambiar(BiConsumer<CampoFactura, String> alCambiar) {
        for (Map.Entry<CampoFactura, TextInputLayout> entrada : campos.entrySet()) {
            CampoFactura campo = entrada.getKey();
            editor(campo).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int inicio, int cantidad, int despues) { }

                @Override
                public void onTextChanged(CharSequence s, int inicio, int antes, int cantidad) { }

                @Override
                public void afterTextChanged(Editable s) {
                    if (!escribiendo) {
                        tocados.add(campo);
                        alCambiar.accept(campo, s.toString());
                    }
                }
            });
        }
    }

    public void poner(CampoFactura campo, String texto) {
        escribiendo = true;
        editor(campo).setText(texto);
        escribiendo = false;
    }

    public String texto(CampoFactura campo) {
        CharSequence texto = editor(campo).getText();
        return texto == null ? "" : texto.toString();
    }

    public Map<CampoFactura, String> textos() {
        Map<CampoFactura, String> textos = new EnumMap<>(CampoFactura.class);
        for (CampoFactura campo : campos.keySet()) {
            textos.put(campo, texto(campo));
        }
        return textos;
    }

    /** Al tocar Guardar: desde ahora se pintan todos los errores. */
    public void tocarTodos() {
        tocados.addAll(campos.keySet());
    }

    public void pintar(CampoFactura campo, RevisionCampo revision) {
        TextInputLayout vista = campos.get(campo);
        vista.setError(null);
        vista.setHelperText(null);
        vista.setBoxBackgroundColor(color(R.color.color_superficie));
        switch (revision.estado()) {
            case VALIDO:
                bordeEIcono(vista, R.color.color_ok, R.drawable.ic_check, R.string.campo_valido);
                break;
            case REVISAR:
                bordeEIcono(vista, R.color.color_alerta, R.drawable.ic_alerta, R.string.campo_revisar);
                vista.setBoxBackgroundColor(color(R.color.color_alerta_claro));
                vista.setHelperTextColor(ColorStateList.valueOf(color(R.color.color_alerta_oscuro)));
                vista.setHelperText(contexto.getString(campo == CampoFactura.IMPORTE_TOTAL
                        ? R.string.verif_baja_confianza : R.string.verif_baja_confianza_campo,
                        revision.confianzaPorcentaje()));
                break;
            default:
                if (tocados.contains(campo)) {
                    vista.setEndIconMode(TextInputLayout.END_ICON_NONE);
                    vista.setError(revision.mensaje());
                } else {
                    bordeEIcono(vista, R.color.color_borde, 0, 0);
                }
                break;
        }
    }

    /** Los errores que devolvió el dominio al guardar (incluye la fecha de un mes cerrado). */
    public void mostrarErrores(Map<CampoFactura, String> errores) {
        for (Map.Entry<CampoFactura, String> error : errores.entrySet()) {
            TextInputLayout vista = campos.get(error.getKey());
            vista.setEndIconMode(TextInputLayout.END_ICON_NONE);
            vista.setError(error.getValue());
        }
        if (!errores.isEmpty()) {
            campos.get(errores.keySet().iterator().next()).requestFocus();
        }
    }

    private void bordeEIcono(TextInputLayout vista, @ColorRes int colorBorde, int icono, int descripcion) {
        vista.setBoxStrokeColorStateList(ColorStateList.valueOf(color(colorBorde)));
        if (icono == 0) {
            vista.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        }
        vista.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        vista.setEndIconDrawable(icono);
        vista.setEndIconTintList(ColorStateList.valueOf(color(colorBorde == R.color.color_alerta
                ? R.color.color_alerta_oscuro : colorBorde)));
        vista.setEndIconContentDescription(descripcion);
    }

    private EditText editor(CampoFactura campo) {
        return campos.get(campo).getEditText();
    }

    private int color(@ColorRes int id) {
        return ContextCompat.getColor(contexto, id);
    }
}
