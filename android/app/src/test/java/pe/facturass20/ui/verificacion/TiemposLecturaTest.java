package pe.facturass20.ui.verificacion;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** La estimación de P07 sigue a las lecturas reales sin que una sola la mueva del todo. */
public class TiemposLecturaTest {

    @Test
    public void seAcercaAlTiempoRealEnPocasLecturas() {
        // Un teléfono que tarda 60 s, partiendo de los 32 s del Moto G34.
        long estimado = TiemposLectura.SIGUIENTE_MS;
        estimado = TiemposLectura.promediar(estimado, 60_000);
        assertEquals(46_000, estimado);
        estimado = TiemposLectura.promediar(estimado, 60_000);
        estimado = TiemposLectura.promediar(estimado, 60_000);
        assertEquals(56_500, estimado);
    }

    @Test
    public void unaLecturaRaraSoloLoMueveAMedias() {
        assertEquals(46_000, TiemposLectura.promediar(32_000, 60_000));
    }
}
