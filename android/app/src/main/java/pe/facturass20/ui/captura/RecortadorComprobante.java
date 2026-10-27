package pe.facturass20.ui.captura;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encuentra el comprobante dentro de la foto para recortarlo (§6.4): Gemma 4 lleva toda imagen a 280 tokens,
 * así que lo que se quita de mesa se gana en detalle. Es el mismo método con el que se recortaron las fotos
 * del dataset ({@code tunning_gemma4/recortar.py}, {@code detectar_esquinas}), para que el modelo reciba
 * imágenes como las que aprendió.
 *
 * <p>Busca la tinta y no el borde del papel, porque la hoja blanca sobre una mesa clara casi no se
 * distingue: tinta es lo que está al menos {@value #DIFERENCIA_TINTA} niveles más oscuro que la media de su
 * entorno (las sombras suaves no pasan) sobre un fondo claro (descarta la mesa de madera). Luego une las
 * letras en renglones, quita los puntos sueltos y toma el rectángulo girado más chico que encierra la
 * tinta, con un margen.</p>
 *
 * <p>Java puro: recibe los bytes de luminancia, así se prueba sin Android. Las medidas de los filtros son
 * las de {@code recortar.py}, pensadas para una imagen de unos {@value #LADO_DETECCION} px de lado mayor.</p>
 */
public final class RecortadorComprobante {

    /** Lado mayor de la imagen sobre la que se busca la tinta, como en {@code recortar.py}. */
    public static final int LADO_DETECCION = 1024;
    static final int DIFERENCIA_TINTA = 15;
    private static final int RADIO_TINTA = 12;      // ventana de 25 × 25
    private static final int RADIO_FONDO = 20;      // ventana de 41 × 41
    private static final int FONDO_MINIMO = 120;
    private static final int RADIO_CIERRE = 7;      // ventana de 15 × 15
    private static final int AREA_MINIMA = 150;
    private static final double MARGEN = 0.03;
    /** Si la tinta ocupa menos que esto de la foto, no parece un comprobante y no se recorta. */
    private static final double FRACCION_MINIMA = 0.10;

    private RecortadorComprobante() {
    }

    /**
     * Las esquinas del comprobante como fracciones de la imagen: {x, y} de arriba-izquierda, arriba-derecha,
     * abajo-derecha y abajo-izquierda (en ese orden, 8 valores). Nulo si no encuentra tinta suficiente.
     *
     * @param luma luminancia de 0 a 255 (byte sin signo), fila por fila, sin relleno
     */
    public static double[] esquinas(byte[] luma, int ancho, int alto) {
        if (ancho < 3 || alto < 3 || luma.length < ancho * alto) {
            throw new IllegalArgumentException("Imagen demasiado pequeña");
        }
        boolean[] tinta = tinta(luma, ancho, alto);
        tinta = cerrar(tinta, ancho, alto);
        List<double[]> contorno = contornoDeComponentesGrandes(tinta, ancho, alto);
        if (contorno.size() < 3) {
            return null;
        }
        double[][] caja = rectanguloMinimo(envolvente(contorno));
        double areaCaja = area(caja);
        if (areaCaja < FRACCION_MINIMA * ancho * alto) {
            return null;
        }
        return aFracciones(ordenar(ampliar(caja)), ancho, alto);
    }

    /** Más oscuro que su entorno de 25 × 25 y sobre un fondo de 41 × 41 más claro que 120. */
    private static boolean[] tinta(byte[] luma, int ancho, int alto) {
        long[] integral = new long[(ancho + 1) * (alto + 1)];
        for (int y = 0; y < alto; y++) {
            long fila = 0;
            for (int x = 0; x < ancho; x++) {
                fila += luma[y * ancho + x] & 0xFF;
                integral[(y + 1) * (ancho + 1) + x + 1] = integral[y * (ancho + 1) + x + 1] + fila;
            }
        }
        boolean[] tinta = new boolean[ancho * alto];
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int valor = luma[y * ancho + x] & 0xFF;
                if (valor <= media(integral, ancho, alto, x, y, RADIO_TINTA) - DIFERENCIA_TINTA
                        && media(integral, ancho, alto, x, y, RADIO_FONDO) > FONDO_MINIMO) {
                    tinta[y * ancho + x] = true;
                }
            }
        }
        return tinta;
    }

    /** Media de la ventana de radio {@code r} centrada en (x, y), recortada al borde de la imagen. */
    private static double media(long[] integral, int ancho, int alto, int x, int y, int r) {
        int x0 = Math.max(0, x - r);
        int y0 = Math.max(0, y - r);
        int x1 = Math.min(ancho, x + r + 1);
        int y1 = Math.min(alto, y + r + 1);
        return (double) suma(integral, ancho, x0, y0, x1, y1) / ((long) (x1 - x0) * (y1 - y0));
    }

    private static long suma(long[] integral, int ancho, int x0, int y0, int x1, int y1) {
        int paso = ancho + 1;
        return integral[y1 * paso + x1] - integral[y0 * paso + x1] - integral[y1 * paso + x0] + integral[y0 * paso + x0];
    }

    /** Cierre morfológico con una ventana de 15 × 15: dilata y luego erosiona, para unir letras en renglones. */
    private static boolean[] cerrar(boolean[] mascara, int ancho, int alto) {
        boolean[] dilatada = filtrar(mascara, ancho, alto, false);
        return filtrar(dilatada, ancho, alto, true);
    }

    /** Dilata (algún vecino encendido) o erosiona (todos los vecinos dentro de la imagen encendidos). */
    private static boolean[] filtrar(boolean[] mascara, int ancho, int alto, boolean erosionar) {
        long[] integral = new long[(ancho + 1) * (alto + 1)];
        for (int y = 0; y < alto; y++) {
            long fila = 0;
            for (int x = 0; x < ancho; x++) {
                fila += mascara[y * ancho + x] ? 1 : 0;
                integral[(y + 1) * (ancho + 1) + x + 1] = integral[y * (ancho + 1) + x + 1] + fila;
            }
        }
        boolean[] resultado = new boolean[ancho * alto];
        for (int y = 0; y < alto; y++) {
            int y0 = Math.max(0, y - RADIO_CIERRE);
            int y1 = Math.min(alto, y + RADIO_CIERRE + 1);
            for (int x = 0; x < ancho; x++) {
                int x0 = Math.max(0, x - RADIO_CIERRE);
                int x1 = Math.min(ancho, x + RADIO_CIERRE + 1);
                long encendidos = suma(integral, ancho, x0, y0, x1, y1);
                resultado[y * ancho + x] = erosionar ? encendidos == (long) (x1 - x0) * (y1 - y0) : encendidos > 0;
            }
        }
        return resultado;
    }

    /**
     * Los píxeles de las manchas de al menos {@value #AREA_MINIMA} píxeles (8 vecinos), resumidos en el
     * primero y el último de cada fila: para la envolvente convexa no hace falta el resto.
     */
    private static List<double[]> contornoDeComponentesGrandes(boolean[] mascara, int ancho, int alto) {
        int[] etiqueta = new int[ancho * alto];
        List<Integer> areas = new ArrayList<>();
        areas.add(0);
        int[] pila = new int[ancho * alto];
        for (int inicio = 0; inicio < mascara.length; inicio++) {
            if (!mascara[inicio] || etiqueta[inicio] != 0) {
                continue;
            }
            int actual = areas.size();
            int area = 0;
            int tope = 0;
            pila[tope++] = inicio;
            etiqueta[inicio] = actual;
            while (tope > 0) {
                int p = pila[--tope];
                area++;
                int px = p % ancho;
                int py = p / ancho;
                for (int dy = -1; dy <= 1; dy++) {
                    int ny = py + dy;
                    if (ny < 0 || ny >= alto) {
                        continue;
                    }
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = px + dx;
                        if (nx < 0 || nx >= ancho) {
                            continue;
                        }
                        int q = ny * ancho + nx;
                        if (mascara[q] && etiqueta[q] == 0) {
                            etiqueta[q] = actual;
                            pila[tope++] = q;
                        }
                    }
                }
            }
            areas.add(area);
        }
        List<double[]> puntos = new ArrayList<>();
        for (int y = 0; y < alto; y++) {
            int primero = -1;
            int ultimo = -1;
            for (int x = 0; x < ancho; x++) {
                int e = etiqueta[y * ancho + x];
                if (e != 0 && areas.get(e) >= AREA_MINIMA) {
                    if (primero < 0) {
                        primero = x;
                    }
                    ultimo = x;
                }
            }
            if (primero >= 0) {
                puntos.add(new double[]{primero, y});
                if (ultimo != primero) {
                    puntos.add(new double[]{ultimo, y});
                }
            }
        }
        return puntos;
    }

    /** Envolvente convexa (cadena monótona de Andrew), en sentido antihorario. */
    static List<double[]> envolvente(List<double[]> puntos) {
        double[][] p = puntos.toArray(new double[0][]);
        Arrays.sort(p, (a, b) -> a[0] != b[0] ? Double.compare(a[0], b[0]) : Double.compare(a[1], b[1]));
        double[][] h = new double[2 * p.length][];
        int k = 0;
        for (double[] q : p) {
            while (k >= 2 && cruz(h[k - 2], h[k - 1], q) <= 0) {
                k--;
            }
            h[k++] = q;
        }
        for (int i = p.length - 2, inferior = k + 1; i >= 0; i--) {
            while (k >= inferior && cruz(h[k - 2], h[k - 1], p[i]) <= 0) {
                k--;
            }
            h[k++] = p[i];
        }
        return new ArrayList<>(Arrays.asList(h).subList(0, Math.max(1, k - 1)));
    }

    private static double cruz(double[] o, double[] a, double[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }

    /** Rectángulo de área mínima que contiene la envolvente: uno de sus lados está sobre un lado de ella. */
    static double[][] rectanguloMinimo(List<double[]> envolvente) {
        double mejorArea = Double.MAX_VALUE;
        double[][] mejor = null;
        int n = envolvente.size();
        for (int i = 0; i < n; i++) {
            double[] a = envolvente.get(i);
            double[] b = envolvente.get((i + 1) % n);
            double largo = Math.hypot(b[0] - a[0], b[1] - a[1]);
            if (largo == 0) {
                continue;
            }
            double ux = (b[0] - a[0]) / largo;
            double uy = (b[1] - a[1]) / largo;
            double minU = Double.MAX_VALUE;
            double maxU = -Double.MAX_VALUE;
            double minV = Double.MAX_VALUE;
            double maxV = -Double.MAX_VALUE;
            for (double[] p : envolvente) {
                double u = p[0] * ux + p[1] * uy;
                double v = -p[0] * uy + p[1] * ux;
                minU = Math.min(minU, u);
                maxU = Math.max(maxU, u);
                minV = Math.min(minV, v);
                maxV = Math.max(maxV, v);
            }
            double area = (maxU - minU) * (maxV - minV);
            if (area < mejorArea) {
                mejorArea = area;
                mejor = new double[][]{
                        punto(minU, minV, ux, uy), punto(maxU, minV, ux, uy),
                        punto(maxU, maxV, ux, uy), punto(minU, maxV, ux, uy)};
            }
        }
        if (mejor == null) {  // todos los puntos iguales
            double[] p = envolvente.get(0);
            mejor = new double[][]{p, p, p, p};
        }
        return mejor;
    }

    private static double[] punto(double u, double v, double ux, double uy) {
        return new double[]{u * ux - v * uy, u * uy + v * ux};
    }

    private static double area(double[][] caja) {
        return Math.hypot(caja[1][0] - caja[0][0], caja[1][1] - caja[0][1])
                * Math.hypot(caja[3][0] - caja[0][0], caja[3][1] - caja[0][1]);
    }

    /** Agranda la caja un {@value #MARGEN} por lado desde su centro. */
    private static double[][] ampliar(double[][] caja) {
        double cx = 0;
        double cy = 0;
        for (double[] p : caja) {
            cx += p[0] / 4;
            cy += p[1] / 4;
        }
        double factor = 1 + 2 * MARGEN;
        double[][] resultado = new double[4][];
        for (int i = 0; i < 4; i++) {
            resultado[i] = new double[]{cx + (caja[i][0] - cx) * factor, cy + (caja[i][1] - cy) * factor};
        }
        return resultado;
    }

    /** Arriba-izquierda (menor x + y), arriba-derecha (menor y − x), abajo-derecha y abajo-izquierda. */
    static double[][] ordenar(double[][] p) {
        double[] ai = p[0];
        double[] bd = p[0];
        double[] ad = p[0];
        double[] bi = p[0];
        for (double[] q : p) {
            if (q[0] + q[1] < ai[0] + ai[1]) {
                ai = q;
            }
            if (q[0] + q[1] > bd[0] + bd[1]) {
                bd = q;
            }
            if (q[1] - q[0] < ad[1] - ad[0]) {
                ad = q;
            }
            if (q[1] - q[0] > bi[1] - bi[0]) {
                bi = q;
            }
        }
        return new double[][]{ai, ad, bd, bi};
    }

    private static double[] aFracciones(double[][] caja, int ancho, int alto) {
        double[] resultado = new double[8];
        for (int i = 0; i < 4; i++) {
            resultado[2 * i] = Math.max(0, Math.min(ancho - 1, caja[i][0])) / (ancho - 1);
            resultado[2 * i + 1] = Math.max(0, Math.min(alto - 1, caja[i][1])) / (alto - 1);
        }
        return resultado;
    }
}
