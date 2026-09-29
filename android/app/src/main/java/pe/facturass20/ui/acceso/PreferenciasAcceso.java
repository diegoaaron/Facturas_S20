package pe.facturass20.ui.acceso;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.biometric.BiometricManager;

/**
 * Preferencias del acceso en {@code SharedPreferences} privadas: si se ingresa con huella y los fallos
 * de PIN de {@link ControlIntentos}. No guardan el PIN (su hash está en la base) ni otro dato del negocio.
 */
public final class PreferenciasAcceso implements ControlIntentos.Almacen {

    private static final String ARCHIVO = "acceso";
    private static final String HUELLA = "huella_activada";
    private static final String FALLOS = "fallos_pin";
    private static final String BLOQUEADO_HASTA = "bloqueado_hasta";

    /** Solo huella o rostro de clase 3 (documentación técnica §10). */
    public static final int AUTENTICADORES = BiometricManager.Authenticators.BIOMETRIC_STRONG;

    private final Context contexto;
    private final SharedPreferences preferencias;

    public PreferenciasAcceso(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.preferencias = this.contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    /** El teléfono tiene un lector de huella con al menos una huella registrada. */
    public boolean huellaDisponible() {
        return BiometricManager.from(contexto).canAuthenticate(AUTENTICADORES) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /** El usuario la activó y el teléfono todavía la admite. */
    public boolean huellaActivada() {
        return preferencias.getBoolean(HUELLA, false) && huellaDisponible();
    }

    public void activarHuella(boolean activar) {
        preferencias.edit().putBoolean(HUELLA, activar).apply();
    }

    public ControlIntentos controlIntentos() {
        return new ControlIntentos(this, System::currentTimeMillis);
    }

    @Override
    public int fallos() {
        return preferencias.getInt(FALLOS, 0);
    }

    @Override
    public long bloqueadoHasta() {
        return preferencias.getLong(BLOQUEADO_HASTA, 0);
    }

    @Override
    public void guardar(int fallos, long bloqueadoHasta) {
        // commit y no apply: si la app se cierra justo después de un fallo, el fallo debe quedar.
        preferencias.edit().putInt(FALLOS, fallos).putLong(BLOQUEADO_HASTA, bloqueadoHasta).commit();
    }
}
