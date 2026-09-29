package pe.facturass20.ui.captura;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferencias de la captura en {@code SharedPreferences} privadas: si se salta la guía P05. */
public final class PreferenciasCaptura {

    private static final String ARCHIVO = "captura";
    private static final String OMITIR_GUIA = "omitir_guia";

    private final SharedPreferences preferencias;

    public PreferenciasCaptura(Context contexto) {
        preferencias = contexto.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    /** El usuario marcó «No volver a mostrar»: Escanear abre directamente la cámara. */
    public boolean omitirGuia() {
        return preferencias.getBoolean(OMITIR_GUIA, false);
    }

    public void omitirGuia(boolean omitir) {
        preferencias.edit().putBoolean(OMITIR_GUIA, omitir).apply();
    }
}
