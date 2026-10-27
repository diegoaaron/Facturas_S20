package pe.facturass20.exportacion;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import pe.facturass20.dominio.modelo.DatosExportacion;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ResumenExportacion;
import pe.facturass20.dominio.puertos.ExportadorArchivos;
import pe.facturass20.dominio.reglas.Montos;

/**
 * Arma el ZIP de P20 (documentación técnica §9.2): {@code facturas.csv}, {@code periodos.csv},
 * {@code determinaciones.csv} y, si se pidieron, {@code imagenes/} y {@code reportes/}. Las fotos se
 * descifran de una en una y van directo al ZIP, sin archivos en claro.
 */
public final class ExportadorDatos implements ExportadorArchivos {

    /** Lo que no está en el dominio y se lee aparte (base, fotos cifradas, PDF del mes). */
    public interface Complementos {

        /** Nula si no se sabe. */
        LocalDateTime fechaRegistro(long idFactura);

        /** Versión de los parámetros del NRUS con que se calculó el mes; nula si no se sabe. */
        String versionParametros(YearMonth periodo);

        /** Nula si no se sabe. */
        LocalDateTime fechaCalculo(YearMonth periodo);

        /** El JPEG descifrado de la factura, o nulo si no tiene foto o no se pudo leer. */
        byte[] imagen(long idFactura);

        /** El PDF del mes (§8.5), o nulo si no se pudo generar. */
        File reporte(YearMonth periodo);
    }

    private final Complementos complementos;

    public ExportadorDatos(Complementos complementos) {
        this.complementos = complementos;
    }

    @Override
    public ResumenExportacion exportar(DatosExportacion datos, OutputStream destino) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(destino));
        // Las fotos van primero: así se sabe qué ruta poner en archivo_imagen sin tenerlas todas en memoria.
        Map<Long, String> imagenes = datos.solicitud().incluirImagenes() ? imagenes(datos, zip) : Map.of();
        int reportes = datos.solicitud().incluirReporte() ? reportes(datos, zip) : 0;

        int facturas = 0;
        BigDecimal totalVigente = Montos.CERO;
        EscritorCsv csv = entrada(zip, "facturas.csv");
        csv.fila("anio", "mes", "fecha_emision", "ruc_emisor", "razon_social", "serie", "numero", "moneda",
                "importe_total", "origen", "estado", "motivo_anulacion", "fecha_registro", "archivo_imagen");
        for (PeriodoMensual periodo : datos.periodos()) {
            for (FacturaCompra f : ordenadas(periodo)) {
                csv.fila(periodo.anio(), periodo.mes(), f.fechaEmision(), f.emisor().ruc(), f.emisor().razonSocial(),
                        f.serie(), f.numero(), f.moneda(), f.importeTotal(), f.origen(), f.estado(),
                        f.motivoAnulacion(), f.id() == null ? null : complementos.fechaRegistro(f.id()),
                        f.id() == null ? null : imagenes.get(f.id()));
                facturas++;
                if (f.esVigente()) {
                    totalVigente = totalVigente.add(f.importeTotal());
                }
            }
        }
        cerrar(zip, csv);

        csv = entrada(zip, "periodos.csv");
        csv.fila("anio", "mes", "estado", "total_ventas", "total_adquisiciones", "facturas_vigentes",
                "facturas_anuladas");
        for (PeriodoMensual periodo : datos.periodos()) {
            int vigentes = periodo.facturasVigentes().size();
            csv.fila(periodo.anio(), periodo.mes(), periodo.estado(),
                    periodo.ventasRegistradas() ? periodo.totalVentas() : null, periodo.totalAdquisiciones(),
                    vigentes, periodo.facturas().size() - vigentes);
        }
        cerrar(zip, csv);

        csv = entrada(zip, "determinaciones.csv");
        csv.fila("anio", "mes", "total_adquisiciones", "monto_determinante", "categoria", "cuota",
                "fecha_vencimiento", "nivel_alerta", "version_parametros", "fecha_calculo");
        for (PeriodoMensual periodo : datos.periodos()) {
            Determinacion d = datos.determinaciones().get(periodo.periodo());
            if (d != null) {
                csv.fila(periodo.anio(), periodo.mes(), d.totalAdquisiciones(), d.montoDeterminante(),
                        d.categoria() == null ? null : d.categoria().codigo(), d.cuota(), d.fechaVencimiento(),
                        d.nivelAlerta(), complementos.versionParametros(periodo.periodo()),
                        complementos.fechaCalculo(periodo.periodo()));
            }
        }
        cerrar(zip, csv);

        zip.finish();
        zip.flush();
        return new ResumenExportacion(facturas, imagenes.size(), reportes, totalVigente);
    }

    /** Escribe las fotos en {@code imagenes/} y devuelve la ruta de cada una por id de factura. */
    private Map<Long, String> imagenes(DatosExportacion datos, ZipOutputStream zip) throws IOException {
        Map<Long, String> rutas = new HashMap<>();
        Set<String> usadas = new HashSet<>();
        for (PeriodoMensual periodo : datos.periodos()) {
            for (FacturaCompra f : ordenadas(periodo)) {
                if (f.id() == null) {
                    continue;
                }
                byte[] jpeg = complementos.imagen(f.id());
                if (jpeg == null) {
                    continue;
                }
                String nombre = periodo.periodo() + "_" + f.emisor().ruc() + "_" + f.serie() + "-" + f.numero();
                if (!usadas.add(nombre)) {
                    nombre = nombre + "_" + f.id();   // una anulada y otra con el mismo número
                    usadas.add(nombre);
                }
                String ruta = "imagenes/" + nombre + ".jpg";
                zip.putNextEntry(new ZipEntry(ruta));
                zip.write(jpeg);
                zip.closeEntry();
                rutas.put(f.id(), ruta);
            }
        }
        return rutas;
    }

    /** Copia el PDF de cada mes en {@code reportes/}; los que no se pudieron generar se omiten. */
    private int reportes(DatosExportacion datos, ZipOutputStream zip) throws IOException {
        int escritos = 0;
        for (PeriodoMensual periodo : datos.periodos()) {
            File pdf = complementos.reporte(periodo.periodo());
            if (pdf == null || !pdf.isFile()) {
                continue;
            }
            zip.putNextEntry(new ZipEntry("reportes/facturas-s20_" + periodo.periodo() + ".pdf"));
            try (InputStream entrada = new FileInputStream(pdf)) {
                byte[] bloque = new byte[64 * 1024];
                for (int n; (n = entrada.read(bloque)) > 0; ) {
                    zip.write(bloque, 0, n);
                }
            }
            zip.closeEntry();
            escritos++;
        }
        return escritos;
    }

    /** Por fecha de emisión y, el mismo día, en el orden en que se registraron. */
    private static List<FacturaCompra> ordenadas(PeriodoMensual periodo) {
        List<FacturaCompra> facturas = new ArrayList<>(periodo.facturas());
        facturas.sort(Comparator.comparing(FacturaCompra::fechaEmision)
                .thenComparing(f -> f.id() == null ? Long.MAX_VALUE : f.id()));
        return facturas;
    }

    private static EscritorCsv entrada(ZipOutputStream zip, String nombre) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        return new EscritorCsv(zip);
    }

    private static void cerrar(ZipOutputStream zip, EscritorCsv csv) throws IOException {
        csv.terminar();
        zip.closeEntry();
    }
}
