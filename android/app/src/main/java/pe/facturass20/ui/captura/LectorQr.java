package pe.facturass20.ui.captura;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import pe.facturass20.dominio.modelo.DatosQr;
import pe.facturass20.dominio.reglas.QrSunat;

/**
 * Busca el QR del comprobante con ML Kit, sin conexión. Va sobre la foto a resolución completa: en las
 * 120 fotos del dataset (4080 × 3072) lee el del comprobante en 39, a la mitad en 34, y ~160 ms por foto
 * en el Moto G34 ({@code LecturaQrDatasetTest}, 2026-10-27); a 896 px casi no se lee. Si no hay QR o falla
 * la lectura, la foto sigue igual y Gemma lee todos los campos.
 */
final class LectorQr {

    private static final String ETIQUETA = "FacturasS20";
    /** Hasta este lado mayor se busca el QR sin achicar la foto; más grande solo gasta memoria. */
    static final int LADO_MAXIMO = 4096;
    private static final long LIMITE_MS = 5_000;

    private LectorQr() {
    }

    /** Lo que trae el QR del comprobante, o {@code null} si no se encontró. Bloquea: en un hilo de fondo. */
    static DatosQr leer(Bitmap foto) {
        BarcodeScannerOptions opciones = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        try (BarcodeScanner lector = BarcodeScanning.getClient(opciones)) {
            List<Barcode> codigos = Tasks.await(lector.process(InputImage.fromBitmap(foto, 0)),
                    LIMITE_MS, TimeUnit.MILLISECONDS);
            List<String> textos = new ArrayList<>();
            for (Barcode codigo : codigos) {
                if (codigo.getRawValue() != null) {
                    textos.add(codigo.getRawValue());
                }
            }
            DatosQr datos = QrSunat.primero(textos).orElse(null);
            Log.i(ETIQUETA, "QR: " + codigos.size() + " encontrados, " + (datos == null ? "ninguno" : "uno")
                    + " del comprobante");
            return datos;
        } catch (ExecutionException | TimeoutException e) {
            Log.w(ETIQUETA, "No se pudo buscar el QR", e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /** Mayor potencia de 2 que deja el lado mayor en {@link #LADO_MAXIMO} px o menos. */
    static int submuestreo(int ladoMayor) {
        int muestra = 1;
        while (ladoMayor / muestra > LADO_MAXIMO) {
            muestra *= 2;
        }
        return muestra;
    }
}
