package pe.facturass20.inferencia;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

/**
 * Achica la foto antes de pasarla al modelo. La foto se guarda a 896 px (§6.4). Gemma 4 lleva toda imagen
 * a 280 tokens como máximo (unos 700 × 930 px), así que con 768 px aprovecha casi todo el detalle: el
 * afinado {@code lora_v2_768} lee mejor que a 512 px y en el Moto G34 tarda lo mismo, ~32 s (medición
 * del 2026-10-27).
 */
final class ReductorFoto {

    /** Lado mayor de la imagen que recibe el modelo. */
    static final int LADO_MODELO = 768;
    private static final int CALIDAD_JPEG = 90;

    private ReductorFoto() { }

    /** La foto con el lado mayor reducido a {@code lado} px, o la misma si ya es menor o no se puede leer. */
    static byte[] reducir(byte[] jpeg, int lado) {
        BitmapFactory.Options limites = new BitmapFactory.Options();
        limites.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, limites);
        int mayor = Math.max(limites.outWidth, limites.outHeight);
        if (mayor <= lado) {
            return jpeg;
        }
        Bitmap original = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        if (original == null) {
            return jpeg;
        }
        float escala = (float) lado / mayor;
        Bitmap reducida = Bitmap.createScaledBitmap(original, Math.round(original.getWidth() * escala),
                Math.round(original.getHeight() * escala), true);
        original.recycle();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        reducida.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida);
        reducida.recycle();
        return salida.toByteArray();
    }
}
