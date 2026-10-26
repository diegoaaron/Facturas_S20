package pe.facturass20.trabajos;

import android.app.Notification;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import pe.facturass20.App;
import pe.facturass20.R;
import pe.facturass20.inferencia.GestorModeloLocal;
import pe.facturass20.inferencia.ManifiestoModelo;

/**
 * Descarga única del modelo (documentación técnica §6.6, pasos 3 a 6): solo por Wi-Fi, reanudable con
 * {@code Range} en {@code filesDir/modelos/<version>.litertlm.part}, y luego verificación e instalación
 * con {@link GestorModeloLocal}. Es la única conexión de red de la app: un {@code GET} anónimo por HTTPS,
 * sin ningún dato del usuario.
 *
 * <p>Si hay una copia del modelo puesta con adb en la carpeta externa de la app, la usa en lugar de
 * descargarla. Corre como servicio en primer plano para que Android no la corte a los 10 minutos.</p>
 */
public final class DescargaModeloWorker extends Worker {

    public static final String NOMBRE = "descarga_modelo";
    public static final String FASE = "fase";
    public static final String HECHOS = "hechos";
    public static final String TOTAL = "total";
    public static final String ERROR = "error";

    public enum Fase { COPIANDO, DESCARGANDO, VERIFICANDO, PROBANDO }

    private static final String ETIQUETA = "FacturasS20";
    private static final int INTENTOS = 3;
    private static final int ID_NOTIFICACION = 20;
    private static final long AVISO_CADA_MS = 500;

    private long ultimoAviso;

    public DescargaModeloWorker(@NonNull Context contexto, @NonNull WorkerParameters parametros) {
        super(contexto, parametros);
    }

    /** Si ya hay una descarga en curso, no empieza otra. */
    public static void encolar(Context contexto) {
        OneTimeWorkRequest pedido = new OneTimeWorkRequest.Builder(DescargaModeloWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .setRequiresStorageNotLow(true)
                        .build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(contexto).enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.KEEP, pedido);
    }

    public static void cancelar(Context contexto) {
        WorkManager.getInstance(contexto).cancelUniqueWork(NOMBRE);
    }

    public static LiveData<List<WorkInfo>> estado(Context contexto) {
        return WorkManager.getInstance(contexto).getWorkInfosForUniqueWorkLiveData(NOMBRE);
    }

    @NonNull
    @Override
    public Result doWork() {
        GestorModeloLocal gestor = ((App) getApplicationContext()).contenedor().gestorModelo();
        ManifiestoModelo manifiesto = gestor.manifiesto();
        if (gestor.estado() == GestorModeloLocal.Estado.LISTO) {
            return Result.success();
        }
        try {
            setForegroundAsync(primerPlano(Fase.DESCARGANDO, 0, manifiesto.tamanoBytes()));
        } catch (IllegalStateException e) {
            // Android no deja pasar a primer plano desde segundo plano; la descarga sigue igual.
            Log.w(ETIQUETA, "La descarga no pudo pasar a primer plano");
        }
        try {
            File copia = gestor.copiaExterna();
            if (copia != null) {
                gestor.copiarAParcial(copia, hechos -> avisar(Fase.COPIANDO, hechos, manifiesto.tamanoBytes()));
            } else {
                descargar(manifiesto, gestor.archivoParcial());
            }
            if (isStopped()) {
                return Result.retry();
            }
            gestor.instalar(hechos -> avisar(hechos < manifiesto.tamanoBytes() ? Fase.VERIFICANDO : Fase.PROBANDO,
                    hechos, manifiesto.tamanoBytes()));
            return Result.success();
        } catch (IOException e) {
            Log.e(ETIQUETA, "Falló la descarga del modelo (intento " + (getRunAttemptCount() + 1) + ")", e);
            if (!isStopped() && getRunAttemptCount() + 1 < INTENTOS) {
                return Result.retry();
            }
            return Result.failure(new Data.Builder()
                    .putString(ERROR, getApplicationContext().getString(R.string.modelo_error_descarga))
                    .build());
        }
    }

    /** Sigue donde quedó el archivo parcial, si el servidor acepta {@code Range}. */
    private void descargar(ManifiestoModelo manifiesto, File parcial) throws IOException {
        long total = manifiesto.tamanoBytes();
        long desde = parcial.length();
        if (desde >= total) {
            if (desde == total) {
                return;
            }
            desde = 0;
        }
        HttpURLConnection conexion = (HttpURLConnection) new URL(manifiesto.url()).openConnection();
        try {
            conexion.setConnectTimeout(30_000);
            conexion.setReadTimeout(60_000);
            conexion.setInstanceFollowRedirects(true);
            if (desde > 0) {
                conexion.setRequestProperty("Range", "bytes=" + desde + "-");
            }
            int codigo = conexion.getResponseCode();
            if (codigo == HttpURLConnection.HTTP_OK) {
                desde = 0;
            } else if (codigo != HttpURLConnection.HTTP_PARTIAL) {
                throw new IOException("El servidor respondió " + codigo);
            }
            byte[] bloque = new byte[1 << 16];
            long hechos = desde;
            try (InputStream entrada = conexion.getInputStream();
                 OutputStream salida = new FileOutputStream(parcial, desde > 0)) {
                for (int n; (n = entrada.read(bloque)) > 0 && !isStopped(); ) {
                    salida.write(bloque, 0, n);
                    hechos += n;
                    avisar(Fase.DESCARGANDO, hechos, total);
                }
            }
        } finally {
            conexion.disconnect();
        }
    }

    /** Publica el avance para P03 y la notificación, como máximo cada medio segundo. */
    private void avisar(Fase fase, long hechos, long total) {
        long ahora = SystemClock.elapsedRealtime();
        if (ahora - ultimoAviso < AVISO_CADA_MS && hechos < total) {
            return;
        }
        ultimoAviso = ahora;
        setProgressAsync(new Data.Builder()
                .putString(FASE, fase.name())
                .putLong(HECHOS, hechos)
                .putLong(TOTAL, total)
                .build());
        try {
            setForegroundAsync(primerPlano(fase, hechos, total));
        } catch (IllegalStateException e) {
            // Sin primer plano: solo se pierde la notificación.
        }
    }

    private ForegroundInfo primerPlano(Fase fase, long hechos, long total) {
        Context contexto = getApplicationContext();
        int texto;
        switch (fase) {
            case VERIFICANDO:
                texto = R.string.modelo_verificando;
                break;
            case PROBANDO:
                texto = R.string.modelo_probando;
                break;
            default:
                texto = R.string.modelo_descargando;
        }
        int porcentaje = total > 0 ? (int) (hechos * 100 / total) : 0;
        Notification aviso = new NotificationCompat.Builder(contexto, App.CANAL_DESCARGA)
                .setSmallIcon(R.drawable.ic_documento)
                .setContentTitle(contexto.getString(R.string.modelo_notificacion_titulo))
                .setContentText(contexto.getString(texto))
                .setProgress(100, porcentaje, fase == Fase.PROBANDO)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new ForegroundInfo(ID_NOTIFICACION, aviso, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }
        return new ForegroundInfo(ID_NOTIFICACION, aviso);
    }
}
