package pe.facturass20.exportacion;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;

import pe.facturass20.dominio.modelo.CategoriaNRUS;
import pe.facturass20.dominio.modelo.DatosExportacion;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.Emisor;
import pe.facturass20.dominio.modelo.EstadoFactura;
import pe.facturass20.dominio.modelo.EstadoPeriodo;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.Moneda;
import pe.facturass20.dominio.modelo.NivelAlerta;
import pe.facturass20.dominio.modelo.OrigenRegistro;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ResumenExportacion;
import pe.facturass20.dominio.modelo.SolicitudExportacion;

/** Contenido del ZIP (documentación técnica §9.2) y criterio 9.5: la suma de facturas.csv = acumulado. */
public class ExportadorDatosTest {

    private static final YearMonth SETIEMBRE = YearMonth.of(2026, 9);
    private static final Emisor EMISOR = new Emisor("20601234565", "DISTRIBUIDORA ANDINA; S.A.C.");
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, 1, 2, 3, (byte) 0xFF, (byte) 0xD9};

    private static FacturaCompra factura(long id, String numero, String importe, int dia, EstadoFactura estado) {
        return new FacturaCompra(id, EMISOR, "F001", numero, LocalDate.of(2026, 9, dia), Moneda.PEN,
                new BigDecimal(importe), OrigenRegistro.IA, estado, estado == EstadoFactura.ANULADA ? "Duplicada" : null);
    }

    private static DatosExportacion datos(boolean imagenes, boolean reporte) {
        PeriodoMensual setiembre = new PeriodoMensual(SETIEMBRE, new BigDecimal("3000.00"), EstadoPeriodo.CERRADO, List.of(
                factura(2, "004822", "200.10", 25, EstadoFactura.VIGENTE),
                factura(1, "004821", "1450.00", 22, EstadoFactura.VIGENTE),
                factura(3, "004823", "99.99", 22, EstadoFactura.ANULADA)));
        Determinacion determinacion = new Determinacion(SETIEMBRE, new BigDecimal("1650.10"), new BigDecimal("3000.00"),
                new CategoriaNRUS(1, new BigDecimal("5000.00"), new BigDecimal("20.00")), new BigDecimal("20.00"),
                LocalDate.of(2026, 10, 15), NivelAlerta.NINGUNA, false);
        return new DatosExportacion(new SolicitudExportacion(2026, 9, imagenes, reporte), List.of(setiembre),
                Map.of(SETIEMBRE, determinacion));
    }

    /** La factura 2 no tiene foto. */
    private static ExportadorDatos exportador(File pdf) {
        return new ExportadorDatos(new ExportadorDatos.Complementos() {
            @Override
            public LocalDateTime fechaRegistro(long idFactura) {
                return LocalDateTime.of(2026, 9, 22, 10, 0).plusMinutes(idFactura);
            }

            @Override
            public String versionParametros(YearMonth periodo) {
                return "2026.1";
            }

            @Override
            public LocalDateTime fechaCalculo(YearMonth periodo) {
                return LocalDateTime.of(2026, 10, 1, 9, 30);
            }

            @Override
            public byte[] imagen(long idFactura) {
                return idFactura == 2 ? null : JPEG;
            }

            @Override
            public File reporte(YearMonth periodo) {
                return pdf;
            }
        });
    }

    private static Map<String, byte[]> abrir(byte[] zip) throws IOException {
        Map<String, byte[]> entradas = new LinkedHashMap<>();
        try (ZipInputStream entrada = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry e; (e = entrada.getNextEntry()) != null; ) {
                entradas.put(e.getName(), entrada.readAllBytes());
            }
        }
        return entradas;
    }

    private static String texto(byte[] csv) {
        return new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
    }

    @Test
    public void conImagenesYReporte() throws IOException {
        File pdf = File.createTempFile("reporte", ".pdf");
        pdf.deleteOnExit();
        Files.write(pdf.toPath(), "%PDF-1.4".getBytes(StandardCharsets.US_ASCII));
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        ResumenExportacion resumen = exportador(pdf).exportar(datos(true, true), salida);

        Map<String, byte[]> zip = abrir(salida.toByteArray());
        assertEquals(List.of("imagenes/2026-09_20601234565_F001-004821.jpg",
                "imagenes/2026-09_20601234565_F001-004823.jpg", "reportes/facturas-s20_2026-09.pdf",
                "facturas.csv", "periodos.csv", "determinaciones.csv"), List.copyOf(zip.keySet()));
        assertArrayEquals(JPEG, zip.get("imagenes/2026-09_20601234565_F001-004821.jpg"));
        assertEquals("%PDF-1.4", new String(zip.get("reportes/facturas-s20_2026-09.pdf"), StandardCharsets.US_ASCII));

        String[] facturas = texto(zip.get("facturas.csv")).split("\r\n");
        assertEquals("anio;mes;fecha_emision;ruc_emisor;razon_social;serie;numero;moneda;importe_total;origen;"
                + "estado;motivo_anulacion;fecha_registro;archivo_imagen", facturas[0]);
        // Por fecha y, el mismo día, por orden de registro.
        assertEquals("2026;9;2026-09-22;20601234565;\"DISTRIBUIDORA ANDINA; S.A.C.\";F001;004821;PEN;1450.00;IA;"
                + "VIGENTE;;2026-09-22 10:01:00;imagenes/2026-09_20601234565_F001-004821.jpg", facturas[1]);
        assertTrue(facturas[2].contains(";ANULADA;Duplicada;"));
        assertTrue(facturas[3].endsWith(";200.10;IA;VIGENTE;;2026-09-22 10:02:00;"));
        assertEquals(4, facturas.length);

        assertEquals("anio;mes;estado;total_ventas;total_adquisiciones;facturas_vigentes;facturas_anuladas\r\n"
                + "2026;9;CERRADO;3000.00;1650.10;2;1\r\n", texto(zip.get("periodos.csv")));
        assertEquals("anio;mes;total_adquisiciones;monto_determinante;categoria;cuota;fecha_vencimiento;"
                + "nivel_alerta;version_parametros;fecha_calculo\r\n"
                + "2026;9;1650.10;3000.00;1;20.00;2026-10-15;NINGUNA;2026.1;2026-10-01 09:30:00\r\n",
                texto(zip.get("determinaciones.csv")));

        assertEquals(3, resumen.facturas());
        assertEquals(2, resumen.imagenes());
        assertEquals(1, resumen.reportes());
        // Criterio 9.5: la suma de las vigentes es el acumulado del mes.
        assertEquals(new BigDecimal("1650.10"), resumen.totalVigente());
    }

    @Test
    public void sinImagenesNiReporte() throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        exportador(null).exportar(datos(false, false), salida);

        Map<String, byte[]> zip = abrir(salida.toByteArray());
        assertEquals(List.of("facturas.csv", "periodos.csv", "determinaciones.csv"), List.copyOf(zip.keySet()));
        for (String fila : texto(zip.get("facturas.csv")).split("\r\n")) {
            assertFalse(fila, fila.contains("imagenes/"));
        }
    }
}
