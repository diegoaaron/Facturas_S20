package pe.facturass20.inferencia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.os.Bundle;
import android.os.Debug;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.inferencia.litert.LiteRtLmPuente;

/**
 * Medición de RNF-04 y RNF-05 en el teléfono (I4, paso 7): carga el modelo, lee la factura de control
 * a varios tamaños y registra el tiempo, la memoria y lo que respondió en {@code medicion_gemma.txt}, dentro de la
 * carpeta externa de la app.
 *
 * <p>El modelo no va en el APK. Se copia antes con adb a
 * {@code /sdcard/Android/data/pe.facturass20/files/modelos/modelo.litertlm}; si no está, la prueba se
 * salta. Como {@code connectedDebugAndroidTest} desinstala la app (y borra el modelo), se corre con
 * {@code am instrument} (ver la guía del equipo).</p>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MedicionGemmaTest {

    private static final String ETIQUETA = "FacturasS20";
    private static final int LECTURAS = 2;
    private static final long LIMITE_MS = 180_000;

    @Test
    public void leeLaFacturaDeControl() throws Exception {
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File modelo = new File(contexto.getExternalFilesDir("modelos"), "modelo.litertlm");
        assumeTrue("Falta el modelo en " + modelo, modelo.isFile());
        byte[] jpeg = leer(contexto.getAssets().open("factura_control.jpg"));

        StringBuilder informe = new StringBuilder();
        anotar(informe, "Modelo: %s (%,d bytes)", modelo.getName(), modelo.length());
        anotar(informe, "Memoria antes de cargar: %d MB", pssMb());

        // -e procesador AUTOMATICO | CPU | CPU_CON_VISION_GPU | GPU, y -e lados 896,512
        Bundle argumentos = InstrumentationRegistry.getArguments();
        LiteRtLmPuente.Modo modo = LiteRtLmPuente.Modo.valueOf(argumentos.getString("procesador", "AUTOMATICO"));
        String[] textoLados = argumentos.getString("lados", "896,512").split(",");
        int[] lados = new int[textoLados.length];
        for (int i = 0; i < lados.length; i++) {
            lados[i] = Integer.parseInt(textoLados[i].trim());
        }
        LiteRtLmPuente motor = new LiteRtLmPuente(contexto.getCacheDir().getAbsolutePath(), modo);
        try {
            long inicio = SystemClock.elapsedRealtime();
            motor.inicializar(modelo.getAbsolutePath());
            anotar(informe, "Carga: %d ms con %s; memoria %d MB", SystemClock.elapsedRealtime() - inicio,
                    motor.getProcesador(), pssMb());

            List<CampoExtraido> campos = null;
            for (int lado : lados) {
                byte[] foto = ReductorFoto.reducir(jpeg, lado);
                for (int i = 1; i <= LECTURAS; i++) {
                    long antes = SystemClock.elapsedRealtime();
                    String respuesta = motor.generar(foto, ParserRespuesta.INSTRUCCION, LIMITE_MS);
                    long ms = SystemClock.elapsedRealtime() - antes;
                    anotar(informe, "Lectura %d a %d px: %d ms; memoria %d MB", i, lado, ms, pssMb());
                    anotar(informe, "  %s", motor.getUltimaMedicion());
                    anotar(informe, "Respuesta:%n%s", respuesta);
                    List<CampoExtraido> leidos = ParserRespuesta.interpretar(respuesta);
                    for (CampoExtraido campo : leidos) {
                        anotar(informe, "  %s = %s (%.2f)", campo.nombre().clave(), campo.valorLeido(), campo.confianza());
                    }
                    guardar(contexto, informe);
                    if (lado == lados[0]) {
                        campos = leidos;
                    }
                }
            }
            assertEquals("20601234565", campos.get(CampoFactura.RUC.ordinal()).valorLeido());
            assertEquals("945.20", campos.get(CampoFactura.IMPORTE_TOTAL.ordinal()).valorLeido());
        } finally {
            guardar(contexto, informe);
            motor.close();
        }
    }

    /** Memoria proporcional del proceso (PSS), que incluye la parte nativa donde vive el modelo. */
    private static long pssMb() {
        return Debug.getPss() / 1024;
    }

    private static void anotar(StringBuilder informe, String formato, Object... datos) {
        String linea = String.format(Locale.ROOT, formato, datos);
        Log.i(ETIQUETA, linea);
        informe.append(linea).append('\n');
    }

    private static void guardar(Context contexto, StringBuilder informe) throws IOException {
        try (FileWriter salida = new FileWriter(new File(contexto.getExternalFilesDir(null), "medicion_gemma.txt"),
                StandardCharsets.UTF_8)) {
            salida.write(informe.toString());
        }
    }

    private static byte[] leer(InputStream entrada) throws IOException {
        try (InputStream in = entrada; ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            byte[] bloque = new byte[8192];
            for (int n; (n = in.read(bloque)) > 0; ) {
                salida.write(bloque, 0, n);
            }
            return salida.toByteArray();
        }
    }
}
