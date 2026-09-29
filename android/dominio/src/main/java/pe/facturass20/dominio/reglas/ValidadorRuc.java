package pe.facturass20.dominio.reglas;

/**
 * Validación del RUC peruano (documentación técnica §5.3): 11 dígitos, prefijo 10, 15, 17 o 20 y
 * dígito verificador módulo 11.
 */
public final class ValidadorRuc {

    private static final int[] PESOS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private ValidadorRuc() { }

    public static boolean esValido(String ruc) {
        if (ruc == null || !ruc.matches("(10|15|17|20)\\d{9}")) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += (ruc.charAt(i) - '0') * PESOS[i];
        }
        int digito = 11 - (suma % 11);
        if (digito == 10) {
            digito = 0;
        } else if (digito == 11) {
            digito = 1;
        }
        return digito == ruc.charAt(10) - '0';
    }
}
