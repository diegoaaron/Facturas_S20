package pe.facturass20.trabajos;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import pe.facturass20.App;
import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.casosuso.RevisarAvisos;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.ui.comun.Formatos;
import pe.facturass20.ui.comun.MainActivity;

/**
 * Notificaciones del NRUS (RF-14, P14 y P15): el vencimiento de la declaración 5 y 1 días antes, el 80 % y
 * el 100 % del límite de la categoría y el tope anual. {@link RevisarAvisos} decide qué toca; aquí solo se
 * arman y se muestran. Corre una vez al día a las 9:00 y después de cada cambio en los montos. No usa red.
 */
public final class RecordatorioWorker extends Worker {

    private static final String ETIQUETA = "FacturasS20";
    private static final String DIARIO = "recordatorios_diarios";
    private static final String AHORA = "revisar_avisos";
    private static final LocalTime HORA = LocalTime.of(9, 0);
    /** El trabajo diario y el de «ahora» pueden coincidir: así no se manda dos veces lo mismo. */
    private static final Object CANDADO = new Object();

    public RecordatorioWorker(@NonNull Context contexto, @NonNull WorkerParameters parametros) {
        super(contexto, parametros);
    }

    /** Programa la revisión diaria; si ya estaba programada, la deja como está. */
    public static void programar(Context contexto) {
        PeriodicWorkRequest diario = new PeriodicWorkRequest.Builder(RecordatorioWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(hastaLaProxima(LocalDateTime.now()).toMinutes(), TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(DIARIO, ExistingPeriodicWorkPolicy.KEEP, diario);
    }

    /** Revisa en seguida, por ejemplo al guardar una factura que pasa el 80 % del límite. */
    public static void revisarAhora(Context contexto) {
        WorkManager.getInstance(contexto).enqueueUniqueWork(AHORA, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(RecordatorioWorker.class));
    }

    static Duration hastaLaProxima(LocalDateTime ahora) {
        LocalDateTime proxima = ahora.toLocalDate().atTime(HORA);
        if (!proxima.isAfter(ahora)) {
            proxima = proxima.plusDays(1);
        }
        return Duration.between(ahora, proxima);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context contexto = getApplicationContext();
        NotificationManagerCompat notificaciones = NotificationManagerCompat.from(contexto);
        // Sin permiso no se revisa: los avisos no quedan anotados y llegan cuando lo conceda, si siguen a tiempo.
        if (!notificaciones.areNotificationsEnabled()) {
            return Result.success();
        }
        ContenedorDependencias c = ((App) contexto).contenedor();
        try {
            List<RevisarAvisos.Pendiente> pendientes;
            synchronized (CANDADO) {
                pendientes = c.revisarAvisos().ejecutar(c.preferenciasAvisos().leer());
            }
            LocalDate hoy = c.reloj().hoy();
            for (RevisarAvisos.Pendiente pendiente : pendientes) {
                mostrar(contexto, notificaciones, c, pendiente, hoy);
            }
        } catch (SecurityException e) {
            Log.w(ETIQUETA, "Sin permiso para notificar");
        } catch (RuntimeException e) {
            // Mañana se vuelve a intentar. No se registra el mensaje: podría traer montos (§10).
            Log.e(ETIQUETA, "No se revisaron los avisos: " + e.getClass().getName());
        }
        return Result.success();
    }

    /** {@link #doWork} ya comprobó que se puede notificar. */
    @SuppressLint("MissingPermission")
    static void mostrar(Context contexto, NotificationManagerCompat notificaciones, ContenedorDependencias c,
            RevisarAvisos.Pendiente pendiente, LocalDate hoy) {
        Determinacion d = pendiente.determinacion();
        String mes = Formatos.mes(d.periodo()) + " de " + d.periodo().getYear();
        String titulo;
        String texto;
        switch (pendiente.aviso().tipo()) {
            case VENCIMIENTO:
                titulo = contexto.getString(R.string.notif_vence_titulo, mes);
                long dias = pendiente.diasParaVencer(hoy);
                String cuando = dias == 0 ? contexto.getString(R.string.notif_vence_hoy)
                        : dias == 1 ? contexto.getString(R.string.notif_vence_manana, Formatos.fechaLarga(d.fechaVencimiento()))
                        : contexto.getString(R.string.notif_vence_en_dias, dias, Formatos.fechaLarga(d.fechaVencimiento()));
                texto = d.cuota() == null ? cuando
                        : cuando + " " + contexto.getString(R.string.notif_vence_cuota, Formatos.moneda(d.cuota()));
                break;
            case TOPE_80:
                titulo = contexto.getString(R.string.notif_80_titulo);
                texto = contexto.getString(R.string.notif_80_texto, d.categoria().codigo(), mes);
                break;
            case TOPE_100:
                if (d.fueraDeRegimen()) {
                    titulo = contexto.getString(R.string.notif_fuera_titulo);
                    texto = contexto.getString(R.string.notif_fuera_texto, mes);
                } else {
                    titulo = contexto.getString(R.string.notif_100_titulo, d.categoria().codigo());
                    texto = contexto.getString(R.string.notif_100_texto, mes, Formatos.moneda(d.cuota()));
                }
                break;
            default:
                titulo = contexto.getString(R.string.notif_tope_anual_titulo);
                texto = contexto.getString(R.string.notif_tope_anual_texto, d.periodo().getYear(),
                        Formatos.monedaCorta(c.parametros().vigentes().topeAnual()));
        }
        Intent abrir = new Intent(contexto, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent alTocar = PendingIntent.getActivity(contexto, 0, abrir,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        // En la pantalla bloqueada no se ven montos ni fechas: solo que hay un aviso (§10).
        Notification publica = new NotificationCompat.Builder(contexto, App.CANAL_AVISOS)
                .setSmallIcon(R.drawable.ic_campana)
                .setContentTitle(contexto.getString(R.string.notif_publica))
                .build();
        Notification aviso = new NotificationCompat.Builder(contexto, App.CANAL_AVISOS)
                .setSmallIcon(R.drawable.ic_campana)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setContentIntent(alTocar)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publica)
                .build();
        // Una notificación por tipo: la nueva reemplaza a la anterior del mismo tipo.
        notificaciones.notify(100 + pendiente.aviso().tipo().ordinal(), aviso);
    }
}
