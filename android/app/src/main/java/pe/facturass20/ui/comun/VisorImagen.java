package pe.facturass20.ui.comun;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pe.facturass20.R;

/** La foto de la factura a pantalla completa (lupa de P08 y toque en la foto de P12). */
public final class VisorImagen {

    private VisorImagen() { }

    public static void mostrar(Context contexto, Bitmap imagen) {
        ImageView vista = new ImageView(contexto);
        vista.setAdjustViewBounds(true);
        vista.setScaleType(ImageView.ScaleType.FIT_CENTER);
        vista.setImageBitmap(imagen);
        vista.setContentDescription(contexto.getString(R.string.foto_factura));
        new MaterialAlertDialogBuilder(contexto)
                .setView(vista)
                .setPositiveButton(R.string.cerrar, null)
                .show();
    }
}
