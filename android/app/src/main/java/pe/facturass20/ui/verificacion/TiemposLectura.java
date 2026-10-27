package pe.facturass20.ui.verificacion;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Cuánto tarda una lectura con Gemma en este teléfono, para el avance de P07. Se guarda en
 * {@code SharedPreferences} privadas y se ajusta con cada lectura, así el porcentaje se acerca al tiempo
 * real aunque el teléfono sea más rápido o más lento que el de referencia.
 *
 * <p>Se distingue la primera lectura después de cargar el modelo, que es más lenta, de las siguientes. Como
 * el modelo se libera al salir de la app, la primera de cada sesión suele ser la lenta.</p>
 */
public final class TiemposLectura {

    /** Primera lectura tras cargar el modelo, medida en el Moto G34 con {@code lora_v2_768} (2026-10-27). */
    static final long PRIMERA_MS = 46_000;
    /** Lecturas siguientes, en el mismo teléfono. */
    static final long SIGUIENTE_MS = 32_000;

    private static final String ARCHIVO = "lectura";
    private static final String CLAVE_PRIMERA = "primera_ms";
    private static final String CLAVE_SIGUIENTE = "siguiente_ms";

    private final SharedPreferences preferencias;

    public TiemposLectura(Context contexto) {
        preferencias = contexto.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    /** @param primera si es la primera lectura después de cargar el modelo */
    public long estimado(boolean primera) {
        return primera ? preferencias.getLong(CLAVE_PRIMERA, PRIMERA_MS)
                : preferencias.getLong(CLAVE_SIGUIENTE, SIGUIENTE_MS);
    }

    /** Suma una lectura que terminó bien (de P07 abierta a la respuesta). */
    public void registrar(boolean primera, long medidoMs) {
        preferencias.edit()
                .putLong(primera ? CLAVE_PRIMERA : CLAVE_SIGUIENTE, promediar(estimado(primera), medidoMs))
                .apply();
    }

    /**
     * La mitad del estimado anterior y la mitad de lo medido: sigue los cambios en dos o tres lecturas sin
     * que una lectura rara lo mueva del todo.
     */
    static long promediar(long anteriorMs, long medidoMs) {
        return (anteriorMs + medidoMs) / 2;
    }
}
