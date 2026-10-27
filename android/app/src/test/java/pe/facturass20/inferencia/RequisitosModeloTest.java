package pe.facturass20.inferencia;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Cuándo un teléfono puede usar el modelo y cuándo hay espacio para descargarlo (§6.6, paso 1). */
public class RequisitosModeloTest {

    private static final long GB = 1024L * 1024 * 1024;

    @Test
    public void necesitaProcesadorDe64BitsYMemoriaSuficiente() {
        // Moto G34: 7,3 GB.
        assertTrue(GestorModeloLocal.compatible(true, 7_300L * 1024 * 1024));
        // Un teléfono de «6 GB» reporta algo menos.
        assertTrue(GestorModeloLocal.compatible(true, GestorModeloLocal.MEMORIA_MINIMA));
        assertFalse(GestorModeloLocal.compatible(true, 4 * GB));
        assertFalse(GestorModeloLocal.compatible(false, 8 * GB));
    }

    @Test
    public void loYaDescargadoCuentaComoEspacio() {
        assertTrue(GestorModeloLocal.espacioSuficiente(GestorModeloLocal.ESPACIO_MINIMO, 0));
        assertFalse(GestorModeloLocal.espacioSuficiente(3 * GB, 0));
        // Al reanudar con 5 GB bajados, bastan 3 GB libres más.
        assertTrue(GestorModeloLocal.espacioSuficiente(3 * GB, 5 * GB));
    }
}
