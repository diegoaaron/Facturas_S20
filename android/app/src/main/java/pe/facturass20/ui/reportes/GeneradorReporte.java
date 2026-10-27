package pe.facturass20.ui.reportes;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.datos.repositorios.ReporteRepositorioRoom;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.ImagenFactura;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.inferencia.Huella;

/**
 * Arma los datos del reporte de un mes y escribe su PDF (§8.5). Lo usan P16 (vista previa, compartir y
 * guardar) y P17, que genera el PDF al cerrar el mes. Se llama desde un hilo de fondo: lee la base y las
 * fotos cifradas.
 */
final class GeneradorReporte {

    /** Carpeta privada de los PDF; el {@code FileProvider} solo da acceso a esta (§10). */
    static final String CARPETA = "reportes";

    private GeneradorReporte() {
    }

    /** Lo que lleva el reporte: el negocio, el mes con su determinación y las facturas vigentes por fecha. */
    static ReporteViewModel.Vista datos(ContenedorDependencias c, YearMonth mes, String sinConfigurar) {
        Contribuyente contribuyente = c.contribuyentes().obtener()
                .orElseThrow(() -> new ReglaNegocioException(sinConfigurar));
        // Si el mes está cerrado, devuelve la determinación congelada (RF-22).
        Determinacion determinacion = c.determinarCategoria().ejecutar(mes);
        PeriodoMensual periodo = c.periodos().buscar(mes).orElseGet(() -> PeriodoMensual.nuevo(mes));
        List<FacturaCompra> facturas = new ArrayList<>(periodo.facturasVigentes());
        facturas.sort(Comparator.comparing(FacturaCompra::fechaEmision));
        int conImagen = 0;
        for (FacturaCompra factura : facturas) {
            if (factura.id() != null && c.facturas().buscarImagen(factura.id()).isPresent()) {
                conImagen++;
            }
        }
        return new ReporteViewModel.Vista(contribuyente, periodo, determinacion, facturas, conImagen);
    }

    /**
     * Escribe el PDF en la carpeta privada (reemplaza el anterior del mismo mes) y lo anota en
     * {@code reporte_mensual} con su SHA-256.
     */
    static File generar(Context contexto, ContenedorDependencias c, ReporteViewModel.Vista datos) throws IOException {
        File carpeta = new File(contexto.getFilesDir(), CARPETA);
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta de reportes");
        }
        File pdf = new File(carpeta, datos.nombreArchivo());
        LocalDateTime ahora = c.reloj().ahora();
        new ReportePdf(contexto).escribir(datos, ahora, factura -> foto(c, factura), pdf);
        c.reportes().registrar(datos.periodo().periodo(), pdf.getAbsolutePath(), Huella.sha256(pdf, null), ahora);
        return pdf;
    }

    /**
     * El PDF generado al cerrar el mes, si el mes está cerrado y el archivo sigue intacto (su SHA-256
     * coincide con el anotado). Así lo que se comparte de un mes cerrado es siempre el mismo documento.
     */
    static File delCierre(ContenedorDependencias c, PeriodoMensual periodo) {
        if (periodo.estaAbierto()) {
            return null;
        }
        Optional<ReporteRepositorioRoom.Generado> ultimo = c.reportes().ultimo(periodo.periodo());
        if (!ultimo.isPresent()) {
            return null;
        }
        File pdf = new File(ultimo.get().ruta());
        try {
            return pdf.isFile() && Huella.sha256(pdf, null).equals(ultimo.get().sha256()) ? pdf : null;
        } catch (IOException e) {
            return null;
        }
    }

    /** La foto descifrada de la factura, o null si no tiene o no se puede leer (el PDF sale sin ella). */
    private static byte[] foto(ContenedorDependencias c, FacturaCompra factura) {
        if (factura.id() == null) {
            return null;
        }
        Optional<ImagenFactura> imagen = c.facturas().buscarImagen(factura.id());
        if (!imagen.isPresent()) {
            return null;
        }
        try {
            return c.almacenImagenes().leer(imagen.get().ruta());
        } catch (RuntimeException e) {
            Log.w("FacturasS20", "Foto no incluida en el reporte: " + e.getClass().getSimpleName());
            return null;
        }
    }
}
