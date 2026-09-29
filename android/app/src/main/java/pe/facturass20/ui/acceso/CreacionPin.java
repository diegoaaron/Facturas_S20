package pe.facturass20.ui.acceso;

import java.util.Arrays;

/**
 * Crear un PIN pidiéndolo dos veces, en P01 (pasos 2 y 3) y al restablecerlo en P02. Guarda el primero
 * hasta compararlo con la repetición.
 */
public final class CreacionPin {

    public enum Resultado {
        /** Se guardó el primero: hay que repetirlo. */
        REPETIR,
        /** La repetición no coincide: se vuelve a empezar. */
        NO_COINCIDE,
        /** Coinciden: {@link #pin()} tiene el PIN nuevo. */
        LISTO
    }

    private char[] primero;
    private char[] confirmado;

    /** {@code true} cuando ya se ingresó el primero y falta repetirlo. */
    public boolean esperandoRepeticion() {
        return primero != null;
    }

    /** Recibe cada PIN completo que escribe el usuario; copia el arreglo. */
    public Resultado ingresar(char[] pin) {
        if (primero == null) {
            primero = pin.clone();
            return Resultado.REPETIR;
        }
        boolean iguales = Arrays.equals(primero, pin);
        Arrays.fill(primero, '0');
        primero = null;
        if (!iguales) {
            return Resultado.NO_COINCIDE;
        }
        confirmado = pin.clone();
        return Resultado.LISTO;
    }

    /** El PIN confirmado, o {@code null} si todavía no hay uno. */
    public char[] pin() {
        return confirmado;
    }

    /** Vuelve al principio y borra lo ingresado. */
    public void reiniciar() {
        if (primero != null) {
            Arrays.fill(primero, '0');
        }
        if (confirmado != null) {
            Arrays.fill(confirmado, '0');
        }
        primero = null;
        confirmado = null;
    }
}
