package pe.facturass20.ui.captura;

/**
 * Posición del marco de encuadre de P06 como fracciones (0 a 1) de la vista previa, tal como la ve el
 * usuario. La cámara entrega las imágenes sin girar (en la orientación del sensor), así que para medir
 * la nitidez dentro del marco o recortar la foto hay que llevar el marco a esas coordenadas.
 *
 * <p>Java puro para probarlo sin Android.</p>
 */
public record MarcoEncuadre(double izquierda, double arriba, double derecha, double abajo) {

    public MarcoEncuadre {
        if (!(0 <= izquierda && izquierda < derecha && derecha <= 1 && 0 <= arriba && arriba < abajo && abajo <= 1)) {
            throw new IllegalArgumentException("Marco fuera de la imagen");
        }
    }

    /** Todo el recorte visible, cuando todavía no se conoce el marco. */
    public static MarcoEncuadre completo() {
        return new MarcoEncuadre(0, 0, 1, 1);
    }

    /**
     * El mismo marco en la imagen sin girar. {@code grados} es lo que hay que girar esa imagen, en
     * sentido horario, para verla derecha ({@code ImageInfo.getRotationDegrees()} de CameraX).
     */
    public MarcoEncuadre enSensor(int grados) {
        switch (((grados % 360) + 360) % 360) {
            case 0:
                return this;
            case 90:
                return new MarcoEncuadre(arriba, 1 - derecha, abajo, 1 - izquierda);
            case 180:
                return new MarcoEncuadre(1 - derecha, 1 - abajo, 1 - izquierda, 1 - arriba);
            case 270:
                return new MarcoEncuadre(1 - abajo, izquierda, 1 - arriba, derecha);
            default:
                throw new IllegalArgumentException("Giro no soportado: " + grados);
        }
    }

    /**
     * Píxeles {@code {izquierda, arriba, derecha, abajo}} del marco dentro del rectángulo visible
     * ({@code x}, {@code y}, {@code ancho}, {@code alto}), en las mismas coordenadas que ese rectángulo.
     */
    public int[] enPixeles(int x, int y, int ancho, int alto) {
        return new int[]{
                x + (int) Math.round(izquierda * ancho),
                y + (int) Math.round(arriba * alto),
                x + (int) Math.round(derecha * ancho),
                y + (int) Math.round(abajo * alto)};
    }
}
