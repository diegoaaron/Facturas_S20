package pe.facturass20.dominio.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import pe.facturass20.dominio.modelo.CampoFactura;

class ValidadorCamposTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "RUC|20601234565",
        "RUC|20 601 234 565",
        "RAZON_SOCIAL|DISTRIBUIDORA ANDINA S.A.C.",
        "SERIE|F001",
        "SERIE|e001",
        "NUMERO|004821",
        "FECHA_EMISION|2026-09-22",
        "FECHA_EMISION|22/09/2026",
        "MONEDA|pen",
        "IMPORTE_TOTAL|1 450,00",
    })
    void aceptaValoresValidos(CampoFactura campo, String valor) {
        assertTrue(ValidadorCampos.error(campo, valor).isEmpty(), () -> campo + " = " + valor);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "RUC|20601234566|El RUC no es válido. Revise los 11 dígitos.",
        "SERIE|F01|La serie tiene 4 letras o números, por ejemplo F001.",
        "NUMERO|123456789|El número tiene de 1 a 8 dígitos.",
        "FECHA_EMISION|31/02/2026|Escriba la fecha como día/mes/año, por ejemplo 22/09/2026.",
        "MONEDA|USD|Por ahora solo se registran compras en soles (S/).",
        "MONEDA|EUR|La moneda debe ser soles (PEN).",
        "IMPORTE_TOTAL|mil|Escriba el importe total, por ejemplo 1450,00.",
        "IMPORTE_TOTAL|0,00|El importe debe ser mayor que cero.",
    })
    void rechazaValoresInvalidos(CampoFactura campo, String valor, String mensaje) {
        assertEquals(mensaje, ValidadorCampos.error(campo, valor).orElseThrow());
    }

    @ParameterizedTest
    @EnumSource(CampoFactura.class)
    void campoVacioOFaltante(CampoFactura campo) {
        assertTrue(ValidadorCampos.error(campo, null).get().startsWith("Falta"));
        assertTrue(ValidadorCampos.error(campo, "   ").get().startsWith("Falta"));
    }

    @Test
    void normaliza() {
        assertEquals("20601234565", ValidadorCampos.normalizar(CampoFactura.RUC, " 20601-234565 "));
        assertEquals("F001", ValidadorCampos.normalizar(CampoFactura.SERIE, "f001"));
        assertEquals("PEN", ValidadorCampos.normalizar(CampoFactura.MONEDA, "pen "));
        assertEquals("ANDINA S.A.C.", ValidadorCampos.normalizar(CampoFactura.RAZON_SOCIAL, " ANDINA   S.A.C. "));
        assertEquals("004821", ValidadorCampos.normalizar(CampoFactura.NUMERO, " 004821"));
        assertNull(ValidadorCampos.normalizar(CampoFactura.NUMERO, null));
    }
}
