package pe.facturass20.ui.captura;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import pe.facturass20.dominio.modelo.DatosQr;

/**
 * Cuántos QR lee ML Kit en las fotos del dataset y si coinciden con las etiquetas. Las fotos están fuera de
 * git: se corre una vez para que cree {@code Android/data/pe.facturass20/files/qr/}, se copian ahí con
 * {@code adb push} junto con {@code etiquetas.csv} y se vuelve a correr; el resumen queda en
 * {@code files/lectura_qr.txt}. Se salta si no están. Se corre con {@code am instrument} (como
 * {@code MedicionGemmaTest}).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LecturaQrDatasetTest {

    private static final String ETIQUETA = "FacturasS20";

    @Test
    public void leeLosQrDelDataset() throws IOException {
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File carpeta = new File(contexto.getExternalFilesDir(null), "qr");
        // La carpeta la crea la app: en una que crea adb, la app no ve los archivos.
        carpeta.mkdirs();
        File etiquetas = new File(carpeta, "etiquetas.csv");
        assumeTrue("No están las fotos del dataset en " + carpeta, etiquetas.exists());

        List<String> lineas = Files.readAllLines(etiquetas.toPath(), StandardCharsets.UTF_8);
        StringBuilder informe = new StringBuilder();
        int fotos = 0;
        int leidos = 0;
        int[] aciertos = new int[5];
        long totalMs = 0;
        for (String linea : lineas.subList(1, lineas.size())) {
            List<String> c = columnas(linea);
            File foto = new File(carpeta, c.get(0));
            if (!foto.exists()) {
                continue;
            }
            fotos++;
            long inicio = SystemClock.elapsedRealtime();
            DatosQr qr = PreparadorFoto.buscarQr(Files.readAllBytes(foto.toPath()), null);
            long ms = SystemClock.elapsedRealtime() - inicio;
            totalMs += ms;
            if (qr == null) {
                informe.append(String.format(Locale.ROOT, "%s %d ms sin QR%n", c.get(0), ms));
                continue;
            }
            leidos++;
            boolean[] bien = {
                qr.ruc() == null || qr.ruc().equals(c.get(1)),
                qr.serie() == null || qr.serie().equals(c.get(3)),
                qr.numero() == null || qr.numero().equals(c.get(4).replaceFirst("^0+(?=\\d)", "")),
                qr.fechaEmision() == null || qr.fechaEmision().toString().equals(c.get(5)),
                // El QR trae el total sin percepción: basta que no pase del etiquetado.
                qr.importeTotal() == null || qr.importeTotal().compareTo(new BigDecimal(c.get(7))) <= 0,
            };
            for (int i = 0; i < bien.length; i++) {
                aciertos[i] += bien[i] ? 1 : 0;
            }
            informe.append(String.format(Locale.ROOT, "%s %d ms %s %s%n", c.get(0), ms, qr,
                    Arrays.toString(bien)));
        }
        String resumen = String.format(Locale.ROOT,
                "%d fotos, QR del comprobante en %d, %d ms por foto. Coinciden (ruc, serie, numero, fecha, total): %s%n",
                fotos, leidos, fotos == 0 ? 0 : totalMs / fotos, Arrays.toString(aciertos));
        Log.i(ETIQUETA, resumen);
        Files.write(new File(contexto.getExternalFilesDir(null), "lectura_qr.txt").toPath(),
                (resumen + informe).getBytes(StandardCharsets.UTF_8));
    }

    /** Una línea de CSV con comillas (la razón social puede llevar comas). */
    private static List<String> columnas(String linea) {
        List<String> columnas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char ch = linea.charAt(i);
            if (ch == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (ch == ',' && !entreComillas) {
                columnas.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(ch);
            }
        }
        columnas.add(actual.toString());
        return columnas;
    }
}
