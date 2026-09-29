package pe.facturass20.dominio.reglas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Casos de la documentación técnica §5.3. */
class ValidadorRucTest {

    @ParameterizedTest
    @ValueSource(strings = {"10456789124", "20601234565", "20512345671", "20498765433", "20610022333", "20455667781"})
    void aceptaRucValidos(String ruc) {
        assertTrue(ValidadorRuc.esValido(ruc));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"10456789123", "30601234565", "2060123456", "20A01234565", "206012345651", ""})
    void rechazaRucInvalidos(String ruc) {
        assertFalse(ValidadorRuc.esValido(ruc));
    }
}
