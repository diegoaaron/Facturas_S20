package pe.facturass20.ui.captura;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * Preprocesado de §6.4: recorta la foto al marco de encuadre, la pone derecha (giro de la cámara o EXIF
 * de la galería), reduce el lado mayor a {@value #LADO_MAYOR} px y la comprime en JPEG calidad
 * {@value #CALIDAD_JPEG}. Luego mide su nitidez y luz con {@link EvaluadorNitidez}.
 *
 * <p>Todo en memoria y en un hilo de fondo; no escribe archivos.</p>
 */
final class PreparadorFoto {

    static final int LADO_MAYOR = 896;
    static final int CALIDAD_JPEG = 90;
    /** Fotos de la galería más grandes que esto se rechazan antes de decodificarlas. */
    private static final int MAXIMO_BYTES_GALERIA = 40 * 1024 * 1024;

    private PreparadorFoto() {
    }

    /**
     * Foto del disparador de P06.
     *
     * @param jpeg    la imagen tal como la entrega {@code ImageCapture}, sin girar
     * @param ancho   ancho de esa imagen según CameraX (para escalar el recorte si el JPEG difiere)
     * @param alto    alto de esa imagen según CameraX
     * @param visible lo que se veía en la vista previa ({@code ImageProxy.getCropRect()})
     * @param giro    grados para verla derecha ({@code getRotationDegrees()})
     * @param marco   el marco de encuadre como fracciones de la vista previa, visto derecho
     */
    static FotoCapturada desdeCamara(byte[] jpeg, int ancho, int alto, Rect visible, int giro,
                                     MarcoEncuadre marco, LocalDateTime fecha) {
        BitmapFactory.Options limites = limites(jpeg);
        double escalaX = (double) limites.outWidth / ancho;
        double escalaY = (double) limites.outHeight / alto;
        int[] recorte = marco.enSensor(giro).enPixeles(
                (int) Math.round(visible.left * escalaX), (int) Math.round(visible.top * escalaY),
                (int) Math.round(visible.width() * escalaX), (int) Math.round(visible.height() * escalaY));
        int ladoRecorte = Math.max(recorte[2] - recorte[0], recorte[3] - recorte[1]);

        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = submuestreo(ladoRecorte);
        Bitmap original = decodificar(jpeg, opciones);
        int m = opciones.inSampleSize;
        int izquierda = limitar(recorte[0] / m, original.getWidth());
        int arriba = limitar(recorte[1] / m, original.getHeight());
        int derecha = limitar(recorte[2] / m, original.getWidth());
        int abajo = limitar(recorte[3] / m, original.getHeight());
        Bitmap lista = transformar(original, izquierda, arriba, derecha - izquierda, abajo - arriba, giro);
        return terminar(lista, fecha, false);
    }

    /** Foto elegida de la galería (RF-03): pasa por el mismo preprocesado, sin recorte. */
    static FotoCapturada desdeGaleria(ContentResolver contenido, Uri uri, LocalDateTime fecha) throws IOException {
        byte[] datos = leer(contenido, uri);
        BitmapFactory.Options limites = limites(datos);
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = submuestreo(Math.max(limites.outWidth, limites.outHeight));
        Bitmap original = decodificar(datos, opciones);
        int giro = new ExifInterface(new ByteArrayInputStream(datos)).getRotationDegrees();
        Bitmap lista = transformar(original, 0, 0, original.getWidth(), original.getHeight(), giro);
        return terminar(lista, fecha, true);
    }

    /** Mayor potencia de 2 que deja el lado mayor en al menos {@link #LADO_MAYOR} px (ahorra memoria). */
    static int submuestreo(int ladoMayor) {
        int muestra = 1;
        while (ladoMayor / (muestra * 2) >= LADO_MAYOR) {
            muestra *= 2;
        }
        return muestra;
    }

    private static Bitmap transformar(Bitmap original, int x, int y, int ancho, int alto, int giro) {
        Matrix matriz = new Matrix();
        matriz.postRotate(giro);
        float escala = Math.min(1f, (float) LADO_MAYOR / Math.max(ancho, alto));
        matriz.postScale(escala, escala);
        Bitmap resultado = Bitmap.createBitmap(original, x, y, ancho, alto, matriz, true);
        if (resultado != original) {
            original.recycle();
        }
        return resultado;
    }

    private static FotoCapturada terminar(Bitmap foto, LocalDateTime fecha, boolean deGaleria) {
        try {
            EvaluadorNitidez.Evaluacion evaluacion =
                    EvaluadorNitidez.evaluar(luminancia(foto), foto.getWidth(), foto.getHeight());
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            foto.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida);
            return new FotoCapturada(salida.toByteArray(), evaluacion, fecha, deGaleria);
        } finally {
            foto.recycle();
        }
    }

    /** Gris con los pesos de BT.601, el mismo que el plano Y de la cámara. */
    private static byte[] luminancia(Bitmap foto) {
        int ancho = foto.getWidth();
        int alto = foto.getHeight();
        int[] pixeles = new int[ancho * alto];
        foto.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto);
        byte[] luma = new byte[pixeles.length];
        for (int i = 0; i < pixeles.length; i++) {
            int p = pixeles[i];
            luma[i] = (byte) ((299 * ((p >> 16) & 0xFF) + 587 * ((p >> 8) & 0xFF) + 114 * (p & 0xFF)) / 1000);
        }
        return luma;
    }

    private static BitmapFactory.Options limites(byte[] datos) {
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(datos, 0, datos.length, opciones);
        if (opciones.outWidth <= 0 || opciones.outHeight <= 0) {
            throw new FotoInvalidaException();
        }
        return opciones;
    }

    private static Bitmap decodificar(byte[] datos, BitmapFactory.Options opciones) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(datos, 0, datos.length, opciones);
        if (bitmap == null) {
            throw new FotoInvalidaException();
        }
        return bitmap;
    }

    private static byte[] leer(ContentResolver contenido, Uri uri) throws IOException {
        try (InputStream entrada = contenido.openInputStream(uri)) {
            if (entrada == null) {
                throw new FotoInvalidaException();
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            byte[] bloque = new byte[64 * 1024];
            int leidos;
            while ((leidos = entrada.read(bloque)) != -1) {
                salida.write(bloque, 0, leidos);
                if (salida.size() > MAXIMO_BYTES_GALERIA) {
                    throw new FotoInvalidaException();
                }
            }
            return salida.toByteArray();
        }
    }

    private static int limitar(int valor, int maximo) {
        return Math.max(0, Math.min(valor, maximo));
    }

    /** El archivo no es una imagen que Android pueda abrir, o es demasiado grande. */
    static final class FotoInvalidaException extends RuntimeException {
    }
}
