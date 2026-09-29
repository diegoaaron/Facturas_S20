package pe.facturass20.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.facturass20.dominio.Datos;
import pe.facturass20.dominio.reglas.Montos;

/** Invariantes de las entidades y objetos de valor de §5.1. */
class ModeloTest {

    @Test
    void contribuyenteValidaRucYDaElUltimoDigito() {
        Contribuyente c = new Contribuyente(Datos.RUC_CONTRIBUYENTE, " Bodega Rosita ", "Rosa Quispe",
                LocalDate.of(2026, 9, 1));
        assertEquals(4, c.ultimoDigitoRuc());
        assertEquals("Bodega Rosita", c.nombre());
        assertThrows(ReglaNegocioException.class,
                () -> new Contribuyente("10456789123", "Bodega", "Rosa", LocalDate.now()));
        assertThrows(ReglaNegocioException.class,
                () -> new Contribuyente(Datos.RUC_CONTRIBUYENTE, " ", "Rosa", LocalDate.now()));
    }

    @Test
    void facturaValidaSusCampos() {
        Emisor emisor = new Emisor(Datos.RUC_EMISOR, "ANDINA");
        LocalDate fecha = LocalDate.of(2026, 9, 22);
        assertThrows(ReglaNegocioException.class, () -> FacturaCompra.nueva(emisor, "F01", "1", fecha,
                Moneda.PEN, Montos.de("1"), OrigenRegistro.IA));
        assertThrows(ReglaNegocioException.class, () -> FacturaCompra.nueva(emisor, "F001", "123456789", fecha,
                Moneda.PEN, Montos.de("1"), OrigenRegistro.IA));
        assertThrows(ReglaNegocioException.class, () -> FacturaCompra.nueva(emisor, "F001", "1", fecha,
                Moneda.PEN, Montos.CERO, OrigenRegistro.IA));
        assertThrows(ReglaNegocioException.class, () -> new FacturaCompra(1L, emisor, "F001", "1", fecha,
                Moneda.PEN, Montos.de("1"), OrigenRegistro.IA, EstadoFactura.ANULADA, null));
    }

    @Test
    void claveUnicaSinCerosDeRelleno() {
        assertEquals("20601234565-F001-4821", Datos.factura("004821", "1.00").claveUnica());
        assertEquals("20601234565-F001-0", FacturaCompra.claveUnica(Datos.RUC_EMISOR, "f001", "0000"));
    }

    @Test
    void idSeAsignaUnaSolaVez() {
        FacturaCompra factura = Datos.factura("1", "1.00");
        factura.asignarId(5);
        assertEquals(5L, factura.id());
        assertThrows(IllegalStateException.class, () -> factura.asignarId(6));
    }

    @Test
    void categoriaAdmiteHastaSuLimite() {
        CategoriaNRUS uno = Datos.parametros().categorias().get(0);
        assertEquals(1, uno.codigo());
        assertTrue(uno.admite(Montos.de("5000.00")));
        assertFalse(uno.admite(Montos.de("5000.01")));
    }

    @Test
    void parametrosOrdenanCategoriasYBuscanVencimiento() {
        ParametrosNrus p = Datos.parametros();
        assertEquals(List.of(1, 2), p.categorias().stream().map(CategoriaNRUS::codigo).toList());
        assertTrue(p.categoriasVigentes(LocalDate.of(2025, 12, 31)).isEmpty());
        assertEquals(LocalDate.of(2026, 10, 15), p.fechaVencimiento(Datos.SEPTIEMBRE, 4).orElseThrow());
        assertTrue(p.fechaVencimiento(Datos.SEPTIEMBRE, 5).isEmpty());
    }

    @Test
    void campoCorregido() {
        CampoExtraido leido = CampoExtraido.leido(CampoFactura.IMPORTE_TOTAL, "1450.00", 0.62);
        assertFalse(leido.corregido());
        assertTrue(leido.conValorFinal("1540.00").corregido());
        assertThrows(IllegalArgumentException.class, () -> CampoExtraido.leido(CampoFactura.RUC, "x", 1.5));
    }

    @Test
    void resultadoExtraccionMarcaCamposARevisar() {
        ResultadoExtraccion r = new ResultadoExtraccion("1.0.0", 12_000, List.of(
                CampoExtraido.leido(CampoFactura.RUC, "20601234565", 0.98),
                CampoExtraido.leido(CampoFactura.RAZON_SOCIAL, "DISTRIBUIDORA ANDINA S.A.C.", 0.95),
                CampoExtraido.leido(CampoFactura.SERIE, "F001", 0.99),
                CampoExtraido.leido(CampoFactura.NUMERO, "004821", 0.97),
                CampoExtraido.leido(CampoFactura.FECHA_EMISION, "2026-09-22", 0.96),
                CampoExtraido.leido(CampoFactura.MONEDA, "PEN", 0.99),
                CampoExtraido.leido(CampoFactura.IMPORTE_TOTAL, "1450.00", 0.62)));
        assertEquals(List.of(CampoFactura.IMPORTE_TOTAL), r.camposARevisar());
        assertEquals(0.62, r.confianzaGlobal());
        assertFalse(r.sugiereOtraFoto());

        ResultadoExtraccion rucMal = r.conValorFinal(CampoFactura.RUC, "20601234566");
        assertEquals(List.of(CampoFactura.RUC, CampoFactura.IMPORTE_TOTAL), rucMal.camposARevisar());
        assertTrue(rucMal.campo(CampoFactura.RUC).orElseThrow().corregido());
    }

    @Test
    void campoFaltanteBajaLaConfianzaGlobal() {
        ResultadoExtraccion r = new ResultadoExtraccion("1.0.0", 1, List.of(
                CampoExtraido.leido(CampoFactura.RUC, "20601234565", 0.98)));
        assertEquals(0.0, r.confianzaGlobal());
        assertTrue(r.sugiereOtraFoto());
        assertEquals(6, r.camposARevisar().size());
        assertFalse(r.camposARevisar().contains(CampoFactura.RUC));
        ResultadoExtraccion conSerie = r.conValorFinal(CampoFactura.SERIE, "F001");
        assertEquals("F001", conSerie.campo(CampoFactura.SERIE).orElseThrow().valorFinal());
    }

    @Test
    void otrosObjetosDeValor() {
        assertTrue(new ImagenFactura("a.jpg.enc", 120, java.time.LocalDateTime.now()).esLegible(100));
        assertTrue(new ModeloLocal("1.0.0", 2600, "ABC", EstadoModelo.ACTIVO, "m").verificarIntegridad("abc"));
        assertTrue(new SolicitudExportacion(2026, null, true, true).esAnual());
        assertThrows(IllegalArgumentException.class, () -> new SolicitudExportacion(2026, 13, true, true));
        assertFalse(new Emisor("123", "X").rucValido());
    }
}
