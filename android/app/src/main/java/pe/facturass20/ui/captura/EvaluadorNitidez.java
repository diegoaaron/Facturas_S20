package pe.facturass20.ui.captura;

/**
 * Mide si una imagen se puede leer (RF-02, documentación técnica §6.4): la nitidez es la varianza del
 * Laplaciano sobre la imagen en gris y la luz, la media de la luminancia. Java puro: recibe los bytes de
 * luminancia (el plano Y de la cámara o la foto ya convertida a gris), así se prueba sin Android.
 *
 * <p>La varianza depende de la escala: por eso se mide sobre el recorte del marco con un tamaño parecido
 * al de la foto final (lado mayor de unos 900 px), tanto en vivo como al disparar.</p>
 */
public final class EvaluadorNitidez {

    /** Umbral inicial de §6.4; hay que calibrarlo con el teléfono de referencia. */
    public static final double UMBRAL_NITIDEZ = 100;
    public static final double LUZ_MINIMA = 60;
    public static final double LUZ_MAXIMA = 200;

    private EvaluadorNitidez() {
    }

    /**
     * Evalúa la región {@code [izquierda, derecha) × [arriba, abajo)}.
     *
     * @param luma        luminancia de 0 a 255 (byte sin signo), fila por fila
     * @param pasoFila    bytes entre el inicio de una fila y el de la siguiente (≥ ancho)
     */
    public static Evaluacion evaluar(byte[] luma, int pasoFila, int izquierda, int arriba, int derecha, int abajo) {
        if (derecha - izquierda < 3 || abajo - arriba < 3) {
            throw new IllegalArgumentException("Región demasiado pequeña");
        }
        long sumaLuz = 0;
        for (int y = arriba; y < abajo; y++) {
            int fila = y * pasoFila;
            for (int x = izquierda; x < derecha; x++) {
                sumaLuz += luma[fila + x] & 0xFF;
            }
        }
        double luz = (double) sumaLuz / ((long) (derecha - izquierda) * (abajo - arriba));

        // Laplaciano de 4 vecinos en el interior de la región.
        double suma = 0;
        double sumaCuadrados = 0;
        long puntos = 0;
        for (int y = arriba + 1; y < abajo - 1; y++) {
            int fila = y * pasoFila;
            for (int x = izquierda + 1; x < derecha - 1; x++) {
                int i = fila + x;
                int laplaciano = 4 * (luma[i] & 0xFF)
                        - (luma[i - 1] & 0xFF) - (luma[i + 1] & 0xFF)
                        - (luma[i - pasoFila] & 0xFF) - (luma[i + pasoFila] & 0xFF);
                suma += laplaciano;
                sumaCuadrados += (double) laplaciano * laplaciano;
                puntos++;
            }
        }
        double media = suma / puntos;
        double varianza = sumaCuadrados / puntos - media * media;
        return new Evaluacion(varianza, luz);
    }

    /** Evalúa la imagen completa. */
    public static Evaluacion evaluar(byte[] luma, int ancho, int alto) {
        return evaluar(luma, ancho, 0, 0, ancho, alto);
    }

    public enum Luz { BAJA, BUENA, EXCESIVA }

    /**
     * @param nitidez varianza del Laplaciano; es lo que se guarda en {@code imagen_factura.nitidez}
     * @param luz     luminancia media de 0 a 255
     */
    public record Evaluacion(double nitidez, double luz) {

        public boolean nitidezBuena() {
            return nitidez >= UMBRAL_NITIDEZ;
        }

        public Luz nivelLuz() {
            if (luz < LUZ_MINIMA) {
                return Luz.BAJA;
            }
            return luz > LUZ_MAXIMA ? Luz.EXCESIVA : Luz.BUENA;
        }

        /** Nitidez y luz «buenas»: se puede disparar y la foto pasa a la lectura. */
        public boolean legible() {
            return nitidezBuena() && nivelLuz() == Luz.BUENA;
        }
    }
}
