package pe.facturass20.ui.acceso;

import java.util.function.LongSupplier;

/**
 * Espera tras 3 PIN incorrectos seguidos (P02, RF-19): durante {@link #ESPERA_MS} no se acepta otro PIN.
 * El estado se guarda fuera del proceso para que cerrar la app no se salte la espera.
 */
public final class ControlIntentos {

    public static final int MAXIMO_FALLOS = 3;
    public static final long ESPERA_MS = 30_000L;

    /** Dónde se guardan los fallos y el fin de la espera (en milisegundos del reloj del teléfono). */
    public interface Almacen {
        int fallos();

        long bloqueadoHasta();

        void guardar(int fallos, long bloqueadoHasta);
    }

    private final Almacen almacen;
    private final LongSupplier reloj;

    /** @param reloj milisegundos desde 1970; en la app, {@code System::currentTimeMillis} */
    public ControlIntentos(Almacen almacen, LongSupplier reloj) {
        this.almacen = almacen;
        this.reloj = reloj;
    }

    /**
     * Milisegundos que faltan para poder intentar otra vez, o 0. Nunca más de {@link #ESPERA_MS}, aunque
     * alguien atrase la hora del teléfono.
     */
    public long msRestantes() {
        long restantes = almacen.bloqueadoHasta() - reloj.getAsLong();
        return restantes <= 0 ? 0 : Math.min(restantes, ESPERA_MS);
    }

    public boolean bloqueado() {
        return msRestantes() > 0;
    }

    /** Intentos que quedan antes de la espera. */
    public int intentosRestantes() {
        return MAXIMO_FALLOS - almacen.fallos();
    }

    /** Suma un PIN incorrecto; al tercero empieza la espera y el contador vuelve a cero. */
    public void registrarFallo() {
        int fallos = almacen.fallos() + 1;
        if (fallos >= MAXIMO_FALLOS) {
            almacen.guardar(0, reloj.getAsLong() + ESPERA_MS);
        } else {
            almacen.guardar(fallos, 0);
        }
    }

    /** Tras un ingreso correcto. */
    public void reiniciar() {
        almacen.guardar(0, 0);
    }
}
