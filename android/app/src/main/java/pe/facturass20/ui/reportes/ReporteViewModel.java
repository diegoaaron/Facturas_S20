package pe.facturass20.ui.reportes;

import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.ImagenFactura;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.inferencia.Huella;
import pe.facturass20.ui.comun.BaseViewModel;
import pe.facturass20.ui.comun.Evento;

/**
 * P16 Reporte (RF-15, RF-16): la vista previa con los datos del mes y el PDF (documentación técnica §8.5),
 * que se genera al compartirlo o guardarlo.
 */
public final class ReporteViewModel extends BaseViewModel {

    /** Facturas por página de miniaturas del PDF y filas de la tabla en la primera página. */
    static final int MINIATURAS_POR_PAGINA = 4;
    static final int FILAS_PRIMERA_PAGINA = 20;
    static final int FILAS_POR_PAGINA = 35;

    /**
     * @param facturas  las vigentes, por fecha
     * @param conImagen cuántas de ellas tienen foto
     */
    public record Vista(Contribuyente contribuyente, PeriodoMensual periodo, Determinacion determinacion,
            List<FacturaCompra> facturas, int conImagen) {

        /** {@code facturas-s20_2026-09.pdf} (§8.5). */
        public String nombreArchivo() {
            return "facturas-s20_" + periodo.periodo() + ".pdf";
        }

        /** Páginas que tendrá el PDF: resumen y tabla, más las miniaturas. */
        public int paginas() {
            return ReporteViewModel.paginas(facturas.size(), conImagen);
        }
    }

    public enum Accion { COMPARTIR, GUARDAR }

    /** El PDF ya escrito en {@code filesDir/reportes/}, para compartirlo o guardarlo donde elija el usuario. */
    public record PdfListo(Accion accion, File archivo, String nombre) { }

    /** Carpeta privada de los PDF; el {@code FileProvider} solo da acceso a esta (§10). */
    static final String CARPETA = "reportes";

    private final MutableLiveData<Vista> vista = new MutableLiveData<>();
    private final MutableLiveData<Evento<PdfListo>> pdfListo = new MutableLiveData<>();

    /** Resumen y tabla (con la continuación si no cabe en la primera página), más las miniaturas. */
    static int paginas(int facturas, int conImagen) {
        int tabla = 1 + Math.max(0, (facturas - FILAS_PRIMERA_PAGINA + FILAS_POR_PAGINA - 1) / FILAS_POR_PAGINA);
        return tabla + (conImagen + MINIATURAS_POR_PAGINA - 1) / MINIATURAS_POR_PAGINA;
    }

    public ReporteViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    public LiveData<Vista> vista() {
        return vista;
    }

    public void cargar(String argumentoPeriodo) {
        YearMonth mes = periodoDe(argumentoPeriodo);
        ContenedorDependencias c = contenedor();
        enFondo(() -> {
            Contribuyente contribuyente = c.contribuyentes().obtener()
                    .orElseThrow(() -> new ReglaNegocioException(texto(R.string.error_sin_configurar)));
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
            return new Vista(contribuyente, periodo, determinacion, facturas, conImagen);
        }, vista::setValue);
    }

    /** Se dispara una vez cuando el PDF está listo para la acción pedida. */
    public LiveData<Evento<PdfListo>> pdfListo() {
        return pdfListo;
    }

    /**
     * Escribe el PDF del mes (§8.5) en la carpeta privada, lo anota en {@code reporte_mensual} con su SHA-256
     * y avisa con {@link #pdfListo()}. Cada vez se genera de nuevo, con los datos del momento.
     */
    public void generarPdf(Accion accion) {
        Vista datos = vista.getValue();
        if (datos == null) {
            return;
        }
        ContenedorDependencias c = contenedor();
        Application aplicacion = getApplication();
        enFondo(() -> {
            File carpeta = new File(aplicacion.getFilesDir(), CARPETA);
            if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
                throw new IOException("No se pudo crear la carpeta de reportes");
            }
            File pdf = new File(carpeta, datos.nombreArchivo());
            LocalDateTime ahora = c.reloj().ahora();
            new ReportePdf(aplicacion).escribir(datos, ahora, factura -> foto(c, factura), pdf);
            c.reportes().registrar(datos.periodo().periodo(), pdf.getAbsolutePath(), Huella.sha256(pdf, null), ahora);
            return new PdfListo(accion, pdf, datos.nombreArchivo());
        }, listo -> pdfListo.setValue(new Evento<>(listo)));
    }

    /** Copia el PDF a donde lo eligió el usuario (selector de archivos del sistema). */
    public void guardarEn(File pdf, Uri destino) {
        ContentResolver archivos = getApplication().getContentResolver();
        enFondo(() -> {
            try (InputStream entrada = new FileInputStream(pdf);
                 OutputStream salida = archivos.openOutputStream(destino)) {
                if (salida == null) {
                    throw new IOException("No se pudo abrir el destino");
                }
                byte[] bloque = new byte[64 * 1024];
                for (int n; (n = entrada.read(bloque)) > 0; ) {
                    salida.write(bloque, 0, n);
                }
            }
            return true;
        }, guardado -> mostrarMensaje(texto(R.string.reporte_guardado)));
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
