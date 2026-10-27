package pe.facturass20.ui.captura;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

import pe.facturass20.dominio.modelo.DatosQr;

/**
 * Preprocesado de §6.4: recorta la foto al marco de encuadre, la pone derecha (giro de la cámara o EXIF
 * de la galería), la recorta al comprobante con {@link RecortadorComprobante} (como las fotos del
 * dataset), reduce el lado mayor a {@value #LADO_MAYOR} px y la comprime en JPEG calidad
 * {@value #CALIDAD_JPEG}. Luego mide su nitidez y luz con {@link EvaluadorNitidez}. Antes de reducirla
 * busca el QR del comprobante con {@link LectorQr}, que a 896 px casi nunca se lee.
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
        DatosQr qr = buscarQr(jpeg, new Rect(recorte[0], recorte[1], recorte[2], recorte[3]));

        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = submuestreo(ladoRecorte);
        Bitmap original = decodificar(jpeg, opciones);
        int m = opciones.inSampleSize;
        int izquierda = limitar(recorte[0] / m, original.getWidth());
        int arriba = limitar(recorte[1] / m, original.getHeight());
        int derecha = limitar(recorte[2] / m, original.getWidth());
        int abajo = limitar(recorte[3] / m, original.getHeight());
        Bitmap enderezada = transformar(original, izquierda, arriba, derecha - izquierda, abajo - arriba, giro);
        return terminar(recortarAlComprobante(enderezada), fecha, false, qr);
    }

    /** Foto elegida de la galería (RF-03): pasa por el mismo preprocesado, sin recorte. */
    static FotoCapturada desdeGaleria(ContentResolver contenido, Uri uri, LocalDateTime fecha) throws IOException {
        byte[] datos = leer(contenido, uri);
        BitmapFactory.Options limites = limites(datos);
        DatosQr qr = buscarQr(datos, null);
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = submuestreo(Math.max(limites.outWidth, limites.outHeight));
        Bitmap original = decodificar(datos, opciones);
        int giro = new ExifInterface(new ByteArrayInputStream(datos)).getRotationDegrees();
        Bitmap enderezada = transformar(original, 0, 0, original.getWidth(), original.getHeight(), giro);
        return terminar(recortarAlComprobante(enderezada), fecha, true, qr);
    }

    /** Mayor potencia de 2 que deja el lado mayor en al menos {@link #LADO_MAYOR} px (ahorra memoria). */
    static int submuestreo(int ladoMayor) {
        int muestra = 1;
        while (ladoMayor / (muestra * 2) >= LADO_MAYOR) {
            muestra *= 2;
        }
        return muestra;
    }

    /**
     * El QR de {@code region} de la imagen (toda si es nula), sin reducirla más que a
     * {@link LectorQr#LADO_MAXIMO} px; el giro no importa. Nulo si no hay o no se puede decodificar.
     */
    @SuppressWarnings("deprecation") // newInstance(byte[], int, int, boolean) es la única hasta Android 12
    static DatosQr buscarQr(byte[] imagen, Rect region) {
        Bitmap foto;
        try {
            BitmapRegionDecoder decodificador = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? BitmapRegionDecoder.newInstance(imagen, 0, imagen.length)
                    : BitmapRegionDecoder.newInstance(imagen, 0, imagen.length, false);
            try {
                Rect zona = region != null ? region
                        : new Rect(0, 0, decodificador.getWidth(), decodificador.getHeight());
                if (zona.isEmpty()) {
                    return null;
                }
                BitmapFactory.Options opciones = new BitmapFactory.Options();
                opciones.inSampleSize = LectorQr.submuestreo(Math.max(zona.width(), zona.height()));
                foto = decodificador.decodeRegion(zona, opciones);
            } finally {
                decodificador.recycle();
            }
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
        if (foto == null) {
            return null;
        }
        try {
            return LectorQr.leer(foto);
        } finally {
            foto.recycle();
        }
    }

    /** Recorta y gira, sin reducir: el recorte al comprobante necesita el detalle. */
    private static Bitmap transformar(Bitmap original, int x, int y, int ancho, int alto, int giro) {
        Matrix matriz = new Matrix();
        matriz.postRotate(giro);
        Bitmap resultado = Bitmap.createBitmap(original, x, y, ancho, alto, matriz, true);
        if (resultado != original) {
            original.recycle();
        }
        return resultado;
    }

    /**
     * Corrige la perspectiva del comprobante que encuentra {@link RecortadorComprobante} (en una copia de
     * {@value RecortadorComprobante#LADO_DETECCION} px, como {@code recortar.py}) y deja el lado mayor en
     * {@value #LADO_MAYOR} px. Si no lo encuentra, solo reduce la foto.
     */
    private static Bitmap recortarAlComprobante(Bitmap foto) {
        int ancho = foto.getWidth();
        int alto = foto.getHeight();
        float escalaDeteccion = Math.min(1f, (float) RecortadorComprobante.LADO_DETECCION / Math.max(ancho, alto));
        Bitmap chica = escalaDeteccion < 1
                ? Bitmap.createScaledBitmap(foto, Math.max(3, Math.round(ancho * escalaDeteccion)),
                        Math.max(3, Math.round(alto * escalaDeteccion)), true)
                : foto;
        double[] esquinas = RecortadorComprobante.esquinas(luminancia(chica), chica.getWidth(), chica.getHeight());
        if (chica != foto) {
            chica.recycle();
        }
        if (esquinas == null) {
            Log.i("FacturasS20", "Recorte al comprobante: no se encontró, se usa la foto entera");
            return reducir(foto);
        }
        float[] origen = new float[8];
        for (int i = 0; i < 4; i++) {
            origen[2 * i] = (float) (esquinas[2 * i] * (ancho - 1));
            origen[2 * i + 1] = (float) (esquinas[2 * i + 1] * (alto - 1));
        }
        double anchoPapel = Math.max(distancia(origen, 0, 1), distancia(origen, 3, 2));
        double altoPapel = Math.max(distancia(origen, 0, 3), distancia(origen, 1, 2));
        double escala = Math.min(1, LADO_MAYOR / Math.max(anchoPapel, altoPapel));
        int anchoFinal = Math.max(1, (int) Math.round(anchoPapel * escala));
        int altoFinal = Math.max(1, (int) Math.round(altoPapel * escala));
        float[] destino = {0, 0, anchoFinal, 0, anchoFinal, altoFinal, 0, altoFinal};
        Matrix matriz = new Matrix();
        if (!matriz.setPolyToPoly(origen, 0, destino, 0, 4)) {
            return reducir(foto);
        }
        Bitmap resultado = Bitmap.createBitmap(anchoFinal, altoFinal, Bitmap.Config.ARGB_8888);
        new Canvas(resultado).drawBitmap(foto, matriz, new Paint(Paint.FILTER_BITMAP_FLAG));
        foto.recycle();
        Log.i("FacturasS20", "Recorte al comprobante: " + ancho + "x" + alto + " -> " + anchoFinal + "x" + altoFinal);
        return resultado;
    }

    private static double distancia(float[] puntos, int a, int b) {
        return Math.hypot(puntos[2 * b] - puntos[2 * a], puntos[2 * b + 1] - puntos[2 * a + 1]);
    }

    private static Bitmap reducir(Bitmap foto) {
        float escala = Math.min(1f, (float) LADO_MAYOR / Math.max(foto.getWidth(), foto.getHeight()));
        if (escala >= 1) {
            return foto;
        }
        Bitmap resultado = Bitmap.createScaledBitmap(foto, Math.round(foto.getWidth() * escala),
                Math.round(foto.getHeight() * escala), true);
        foto.recycle();
        return resultado;
    }

    private static FotoCapturada terminar(Bitmap foto, LocalDateTime fecha, boolean deGaleria, DatosQr qr) {
        try {
            EvaluadorNitidez.Evaluacion evaluacion =
                    EvaluadorNitidez.evaluar(luminancia(foto), foto.getWidth(), foto.getHeight());
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            foto.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida);
            return new FotoCapturada(salida.toByteArray(), evaluacion, fecha, deGaleria, qr);
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
