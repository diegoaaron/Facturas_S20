package pe.facturass20.ui.mes;

import android.content.Context;
import android.content.SharedPreferences;

import pe.facturass20.dominio.casosuso.RevisarAvisos;

/**
 * Qué avisos quiere recibir el usuario (P14) y qué recordatorios del vencimiento (P15), en
 * {@code SharedPreferences} privadas. Todos vienen activados. Las notificaciones las manda
 * {@code RecordatorioWorker}.
 */
public final class PreferenciasAvisos {

    /** Cada interruptor de P14 y P15, con el nombre de su preferencia. */
    public enum Aviso {
        LIMITE_80("aviso_80"),
        LIMITE_100("aviso_100"),
        TOPE_ANUAL("aviso_tope_anual"),
        VENCE_EN_5_DIAS("recordatorio_5_dias"),
        VENCE_EN_1_DIA("recordatorio_1_dia");

        private final String clave;

        Aviso(String clave) {
            this.clave = clave;
        }
    }

    private static final String ARCHIVO = "avisos";
    private static final String PERMISO_PEDIDO = "permiso_pedido";

    private final SharedPreferences preferencias;

    public PreferenciasAvisos(Context contexto) {
        preferencias = contexto.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    public boolean activado(Aviso aviso) {
        return preferencias.getBoolean(aviso.clave, true);
    }

    public void activar(Aviso aviso, boolean activado) {
        preferencias.edit().putBoolean(aviso.clave, activado).apply();
    }

    /** Si P04 ya pidió el permiso de notificaciones (se pide una sola vez). */
    public boolean permisoPedido() {
        return preferencias.getBoolean(PERMISO_PEDIDO, false);
    }

    public void marcarPermisoPedido() {
        preferencias.edit().putBoolean(PERMISO_PEDIDO, true).apply();
    }

    /** Las elecciones, tal como las usa {@link RevisarAvisos}. */
    public RevisarAvisos.Preferencias leer() {
        return new RevisarAvisos.Preferencias(activado(Aviso.LIMITE_80), activado(Aviso.LIMITE_100),
                activado(Aviso.TOPE_ANUAL), activado(Aviso.VENCE_EN_5_DIAS), activado(Aviso.VENCE_EN_1_DIA));
    }
}
